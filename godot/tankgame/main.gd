extends Node2D

const ARENA_SIZE := Vector2(960.0, 540.0)
const SERVER_MAP_SIZE := Vector2(920.0, 440.0)
const MAP_ORIGIN := Vector2(20.0, 70.0)
const TANK_SIZE := Vector2(42.0, 30.0)
const TANK_SPEED := 240.0
const BULLET_SPEED := 520.0
const RECONNECT_DELAY := 2.0
const DEBRIS_LIFETIME := 7.0

var player_position := ARENA_SIZE / 2.0
var player_angle := 0.0
var bullets: Array[Dictionary] = []
var targets := [Vector2(180, 150), Vector2(760, 145), Vector2(210, 410), Vector2(750, 390)]
var score := 0
var websocket := WebSocketPeer.new()
var websocket_status := "Connecting to Spring WebSocket..."
var game_over := false
var server_connected := false
var queue_sent := false
var local_tank_id := ""
var current_game_id := ""
var server_tanks: Dictionary = {}
var visual_tanks: Dictionary = {}
var server_projectiles: Array = []
var server_walls: Array = []
var debris: Array[Dictionary] = []
var fire_requested := false
var lobby_visible := true
var reconnect_timer := 0.0
var input_sequence := 0


func _ready() -> void:
	_connect_to_server()
	queue_redraw()


func _process(delta: float) -> void:
	_move_player(delta)
	_update_bullets(delta)
	_poll_websocket(delta)
	_interpolate_server_state(delta)
	_update_debris(delta)
	_send_input()
	queue_redraw()


func _unhandled_input(event: InputEvent) -> void:
	if (
		lobby_visible
		and event.is_pressed()
		and (
			event is InputEventMouseButton
			or (
				event is InputEventKey
				and (event.keycode == KEY_ENTER or event.keycode == KEY_KP_ENTER)
			)
		)
	):
		_queue_for_battle()
		return
	if event is InputEventMouseMotion:
		player_angle = player_position.angle_to_point(event.position)
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
		if game_over:
			_restart()
		else:
			_shoot()
	if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_SPACE:
		fire_requested = true
	if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_R:
		_restart()


func _move_player(delta: float) -> void:
	if server_connected:
		return
	var direction := _movement_direction()
	player_position += direction * TANK_SPEED * delta
	player_position.x = clamp(player_position.x, 32.0, ARENA_SIZE.x - 32.0)
	player_position.y = clamp(player_position.y, 90.0, ARENA_SIZE.y - 32.0)


func _shoot() -> void:
	if game_over:
		return
	if server_connected:
		fire_requested = true
		return
	var muzzle := player_position + Vector2.RIGHT.rotated(player_angle) * 30.0
	bullets.append({"position": muzzle, "direction": Vector2.RIGHT.rotated(player_angle)})


func _update_bullets(delta: float) -> void:
	if server_connected:
		return
	for bullet in bullets:
		bullet.position += bullet.direction * BULLET_SPEED * delta
	for index in range(bullets.size() - 1, -1, -1):
		var bullet: Dictionary = bullets[index]
		var hit_target := false
		for target_index in range(targets.size() - 1, -1, -1):
			if bullet.position.distance_to(targets[target_index]) < 24.0:
				targets.remove_at(target_index)
				score += 1
				hit_target = true
				break
		if hit_target or not Rect2(Vector2.ZERO, ARENA_SIZE).has_point(bullet.position):
			bullets.remove_at(index)
	if targets.is_empty():
		game_over = true


func _restart() -> void:
	player_position = ARENA_SIZE / 2.0
	player_angle = 0.0
	bullets.clear()
	debris.clear()
	targets = [Vector2(180, 150), Vector2(760, 145), Vector2(210, 410), Vector2(750, 390)]
	score = 0
	game_over = false


func _connect_to_server() -> void:
	websocket = WebSocketPeer.new()
	reconnect_timer = 0.0
	var websocket_url := "ws://127.0.0.1:8080/tankgame-ws"
	if OS.has_feature("web"):
		var protocol: String = JavaScriptBridge.eval("location.protocol")
		var host: String = JavaScriptBridge.eval("location.host")
		websocket_url = ("wss://" if protocol == "https:" else "ws://") + host + "/tankgame-ws"
	var error := websocket.connect_to_url(websocket_url)
	if error != OK:
		websocket_status = "WebSocket unavailable (offline prototype still works)"


func _poll_websocket(delta: float) -> void:
	websocket.poll()
	reconnect_timer += delta
	match websocket.get_ready_state():
		WebSocketPeer.STATE_OPEN:
			reconnect_timer = 0.0
			if not server_connected:
				server_connected = true
				websocket_status = "Connected · Lobby ready"
			while websocket.get_available_packet_count() > 0:
				var message := websocket.get_packet().get_string_from_utf8()
				if message.length() > 0:
					_handle_server_message(message)
		WebSocketPeer.STATE_CONNECTING:
			websocket_status = "Connecting to Spring WebSocket..."
		WebSocketPeer.STATE_CLOSED:
			if server_connected or not lobby_visible:
				server_connected = false
				queue_sent = false
				lobby_visible = true
				local_tank_id = ""
				current_game_id = ""
				visual_tanks.clear()
				debris.clear()
				websocket_status = "Connection lost · reconnecting..."
			if reconnect_timer >= RECONNECT_DELAY:
				_connect_to_server()


func _queue_for_battle() -> void:
	if not server_connected or queue_sent:
		return
	websocket.send_text(JSON.stringify({"action": "queue", "playerName": "Pilot"}))
	queue_sent = true
	lobby_visible = false
	websocket_status = "Queued · Waiting for a pilot or AI opponent..."


func _send_input() -> void:
	if not server_connected or local_tank_id.is_empty():
		return
	var mouse_position := get_viewport().get_mouse_position()
	var movement := _movement_direction()
	var input := {
		"up": movement.y < 0.0,
		"down": movement.y > 0.0,
		"left": movement.x < 0.0,
		"right": movement.x > 0.0,
		"shoot":
		(
			fire_requested
			or Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT)
			or Input.is_action_pressed("fire")
		),
		"mouseX": clamp(mouse_position.x - MAP_ORIGIN.x, 0.0, SERVER_MAP_SIZE.x),
		"mouseY": clamp(mouse_position.y - MAP_ORIGIN.y, 0.0, SERVER_MAP_SIZE.y)
	}
	websocket.send_text(
		JSON.stringify(
			{
				"action": "input",
				"gameId": current_game_id,
				"sequence": input_sequence,
				"input": input
			}
		)
	)
	input_sequence += 1
	fire_requested = false


func _movement_direction() -> Vector2:
	var left := (
		Input.is_action_pressed("move_left")
		or Input.is_key_pressed(KEY_A)
		or Input.is_key_pressed(KEY_LEFT)
	)
	var right := (
		Input.is_action_pressed("move_right")
		or Input.is_key_pressed(KEY_D)
		or Input.is_key_pressed(KEY_RIGHT)
	)
	var up := (
		Input.is_action_pressed("move_up")
		or Input.is_key_pressed(KEY_W)
		or Input.is_key_pressed(KEY_UP)
	)
	var down := (
		Input.is_action_pressed("move_down")
		or Input.is_key_pressed(KEY_S)
		or Input.is_key_pressed(KEY_DOWN)
	)
	return Vector2(float(right) - float(left), float(down) - float(up)).normalized()


func _interpolate_server_state(delta: float) -> void:
	if not server_connected:
		return
	var blend := 1.0 - exp(-delta * 18.0)
	for tank_id in server_tanks:
		var target: Dictionary = server_tanks[tank_id]
		if not visual_tanks.has(tank_id):
			visual_tanks[tank_id] = target.duplicate()
			continue
		var visual: Dictionary = visual_tanks[tank_id]
		visual["x"] = lerp(float(visual.get("x", 0.0)), float(target.get("x", 0.0)), blend)
		visual["y"] = lerp(float(visual.get("y", 0.0)), float(target.get("y", 0.0)), blend)
		visual["rotation"] = lerp_angle(
			float(visual.get("rotation", 0.0)), float(target.get("rotation", 0.0)), blend
		)
		for key in ["playerName", "health", "maxHealth", "color", "alive", "kills", "bot"]:
			visual[key] = target.get(key, visual.get(key))
		visual_tanks[tank_id] = visual
	for tank_id in visual_tanks.keys():
		if not server_tanks.has(tank_id):
			visual_tanks.erase(tank_id)
	if visual_tanks.has(local_tank_id):
		var local_tank: Dictionary = visual_tanks[local_tank_id]
		player_position = (
			MAP_ORIGIN
			+ Vector2(
				float(local_tank.get("x", 0.0)) + 20.0, float(local_tank.get("y", 0.0)) + 20.0
			)
		)


func _handle_server_message(message: String) -> void:
	var payload = JSON.parse_string(message)
	if payload == null or not payload is Dictionary:
		return
	match payload.get("type", ""):
		"connected":
			websocket_status = "Connected · Lobby ready"
		"joined":
			local_tank_id = str(payload.get("tankId", ""))
			current_game_id = str(payload.get("gameId", ""))
			lobby_visible = false
			websocket_status = "Queued · Waiting for a pilot or AI opponent..."
		"state":
			var game: Dictionary = payload.get("game", {})
			current_game_id = str(game.get("gameId", current_game_id))
			var next_tanks: Dictionary = game.get("tanks", {})
			for tank_id in server_tanks:
				var previous_tank: Dictionary = server_tanks[tank_id]
				if not next_tanks.has(tank_id) and visual_tanks.has(tank_id):
					_spawn_tank_explosion(_tank_screen_position(visual_tanks[tank_id]))
				elif next_tanks.has(tank_id):
					var next_tank: Dictionary = next_tanks[tank_id]
					if (
						bool(previous_tank.get("alive", true))
						and not bool(next_tank.get("alive", true))
					):
						_spawn_tank_explosion(_tank_screen_position(next_tank))
			server_tanks = next_tanks
			server_projectiles = game.get("projectiles", [])
			server_walls = game.get("walls", [])
			var game_status: String = str(game.get("status", "WAITING"))
			if game_status == "FINISHED":
				lobby_visible = true
				queue_sent = false
				local_tank_id = ""
				current_game_id = ""
				visual_tanks.clear()
				websocket_status = (
					"Battle finished · Winner: " + str(game.get("winnerName", "Unknown"))
				)
			else:
				websocket_status = (
					"Battle live · %d pilots" % server_tanks.size()
					if game_status == "PLAYING"
					else "Queued · Waiting for a pilot or AI opponent..."
				)
			for tank_id in server_tanks:
				if str(tank_id) == local_tank_id:
					player_angle = float(server_tanks[tank_id].get("rotation", 0.0))
		"error":
			websocket_status = "Battle error: " + str(payload.get("message", "Unknown error"))


func _tank_screen_position(tank: Dictionary) -> Vector2:
	return MAP_ORIGIN + Vector2(float(tank.get("x", 0.0)) + 20.0, float(tank.get("y", 0.0)) + 20.0)


func _spawn_tank_explosion(position: Vector2) -> void:
	# Leave a small pile of tank parts at the wreck site. The pieces are
	# deliberately grounded: they do not burst outward, fade immediately, or spin.
	for index in range(9):
		debris.append(
			{
				"position": position + Vector2(randf_range(-22.0, 22.0), randf_range(-16.0, 16.0)),
				"velocity": Vector2.ZERO,
				"life": DEBRIS_LIFETIME,
				"max_life": DEBRIS_LIFETIME,
				"size": randf_range(5.0, 12.0),
				"color": Color("4d5968").lerp(Color("a86545"), randf_range(0.0, 0.35)),
				"rotation": randf_range(0.0, TAU),
				"spin": 0.0,
				"debris": true
			}
		)


func _update_debris(delta: float) -> void:
	for piece in debris:
		piece.life = float(piece.get("life", 0.0)) - delta
	for index in range(debris.size() - 1, -1, -1):
		if float(debris[index].get("life", 0.0)) <= 0.0:
			debris.remove_at(index)


func _draw() -> void:
	draw_rect(Rect2(Vector2.ZERO, ARENA_SIZE), Color("101827"))
	draw_rect(Rect2(18, 70, ARENA_SIZE.x - 36, 440), Color("17243a"), true)
	for x in range(40, 960, 40):
		draw_line(Vector2(x, 100), Vector2(x, 520), Color("20304b"), 1.0)
	for y in range(90, 510, 40):
		draw_line(Vector2(20, y), Vector2(940, y), Color("20304b"), 1.0)

	var font := ThemeDB.fallback_font
	draw_string(
		font,
		Vector2(28, 34),
		"IRONBOUND ONLINE · ACTIVE DEVELOPMENT",
		HORIZONTAL_ALIGNMENT_LEFT,
		-1,
		22,
		Color("e8eef8")
	)
	draw_string(
		font,
		Vector2(28, 62),
		"WASD / arrow keys to move · mouse or space to shoot",
		HORIZONTAL_ALIGNMENT_LEFT,
		-1,
		16,
		Color("9fb2cf")
	)
	draw_string(
		font,
		Vector2(680, 34),
		"Pilots: %d" % server_tanks.size() if server_connected else "Score: %d" % score,
		HORIZONTAL_ALIGNMENT_LEFT,
		-1,
		20,
		Color("ffd166")
	)
	draw_rect(Rect2(0, 510, ARENA_SIZE.x, 30), Color("101827"), true)
	draw_string(
		font, Vector2(28, 530), websocket_status, HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color("8bd5ca")
	)
	if server_connected:
		_draw_server_state(font)
	else:
		for target in targets:
			draw_circle(target, 22.0, Color("e76f51"))
			draw_circle(target, 12.0, Color("f4a261"))
			draw_circle(target, 4.0, Color("264653"))
		for bullet in bullets:
			draw_circle(bullet.position, 5.0, Color("ffd166"))
	if game_over and not server_connected:
		draw_rect(Rect2(260, 220, 440, 100), Color(0.05, 0.09, 0.15, 0.92), true)
		draw_string(
			font,
			Vector2(352, 260),
			"ARENA CLEAR",
			HORIZONTAL_ALIGNMENT_LEFT,
			-1,
			28,
			Color("95e1d3")
		)
		draw_string(
			font,
			Vector2(315, 294),
			"Click or press R to play again",
			HORIZONTAL_ALIGNMENT_LEFT,
			-1,
			16,
			Color("e8eef8")
		)

	if not server_connected and not lobby_visible:
		draw_set_transform(player_position, player_angle)
		draw_rect(
			Rect2(-TANK_SIZE.x / 2.0, -TANK_SIZE.y / 2.0, TANK_SIZE.x, TANK_SIZE.y),
			Color("4ecdc4"),
			true
		)
		draw_rect(Rect2(-6, -5, 32, 10), Color("95e1d3"), true)
		draw_circle(Vector2.ZERO, 9.0, Color("264653"))
		draw_set_transform(Vector2.ZERO, 0.0)

	# Draw the lobby last so the restart dialog stays above walls and tanks.
	if lobby_visible:
		draw_rect(Rect2(250, 165, 460, 220), Color(0.05, 0.09, 0.15, 0.96), true)
		draw_string(
			font,
			Vector2(250, 215),
			"IRONBOUND ONLINE",
			HORIZONTAL_ALIGNMENT_CENTER,
			460,
			28,
			Color("95e1d3")
		)
		draw_string(
			font,
			Vector2(250, 250),
			"MULTIPLAYER TANK RPG",
			HORIZONTAL_ALIGNMENT_CENTER,
			460,
			16,
			Color("e8eef8")
		)
		if server_connected:
			draw_string(
				font,
				Vector2(250, 295),
				"Click or press Enter",
				HORIZONTAL_ALIGNMENT_CENTER,
				460,
				20,
				Color("ffd166")
			)
			draw_string(
				font,
				Vector2(250, 325),
				"to enter the battle lobby",
				HORIZONTAL_ALIGNMENT_CENTER,
				460,
				16,
				Color("9fb2cf")
			)
			draw_string(
				font,
				Vector2(250, 355),
				"An AI opponent joins if no pilot is found",
				HORIZONTAL_ALIGNMENT_CENTER,
				460,
				13,
				Color("9fb2cf")
			)
		else:
			draw_string(
				font,
				Vector2(250, 305),
				"Connecting to battle server...",
				HORIZONTAL_ALIGNMENT_CENTER,
				460,
				18,
				Color("ffd166")
			)
			draw_string(
				font,
				Vector2(250, 340),
				"Please wait",
				HORIZONTAL_ALIGNMENT_CENTER,
				460,
				16,
				Color("9fb2cf")
			)


func _draw_server_state(font: Font) -> void:
	for wall in server_walls:
		var wall_rect := Rect2(
			MAP_ORIGIN + Vector2(float(wall.get("x", 0.0)), float(wall.get("y", 0.0))),
			Vector2(float(wall.get("width", 0.0)), float(wall.get("height", 0.0)))
		)
		draw_rect(wall_rect, Color("52627a"), true)
		draw_rect(wall_rect, Color("8b9bb5"), false, 2.0)
	for projectile in server_projectiles:
		draw_circle(
			MAP_ORIGIN + Vector2(float(projectile.get("x", 0.0)), float(projectile.get("y", 0.0))),
			5.0,
			Color("ffd166")
		)
	for tank_id in visual_tanks:
		var tank: Dictionary = visual_tanks[tank_id]
		var tank_position := (
			MAP_ORIGIN + Vector2(float(tank.get("x", 0.0)) + 20.0, float(tank.get("y", 0.0)) + 20.0)
		)
		var tank_color := Color(str(tank.get("color", "#4ecdc4")))
		if not bool(tank.get("alive", true)):
			tank_color = Color("3b4654")
		draw_set_transform(tank_position, float(tank.get("rotation", 0.0)))
		draw_rect(Rect2(-20.0, -20.0, 40.0, 40.0), tank_color, true)
		draw_rect(Rect2(-6.0, -5.0, 32.0, 10.0), tank_color.lightened(0.35), true)
		draw_circle(Vector2.ZERO, 9.0, Color("264653"))
		draw_set_transform(Vector2.ZERO, 0.0)
		draw_string(
			font,
			tank_position + Vector2(-24.0, -28.0),
			str(tank.get("playerName", "Pilot")),
			HORIZONTAL_ALIGNMENT_LEFT,
			-1,
			12,
			Color("e8eef8")
		)
		draw_string(
			font,
			tank_position + Vector2(-20.0, 38.0),
			"%d HP" % int(tank.get("health", 0)),
			HORIZONTAL_ALIGNMENT_LEFT,
			-1,
			11,
			Color("95e1d3")
		)
	for piece in debris:
		var opacity: float = clampf(
			float(piece.get("life", 0.0)) / float(piece.get("max_life", 1.0)), 0.0, 1.0
		)
		var piece_color: Color = piece.get("color", Color.WHITE)
		piece_color.a = opacity
		draw_set_transform(piece.position, float(piece.get("rotation", 0.0)))
		draw_rect(
			Rect2(-piece.size / 2.0, Vector2(piece.size, piece.size * 0.65)), piece_color, true
		)
		draw_set_transform(Vector2.ZERO, 0.0)
