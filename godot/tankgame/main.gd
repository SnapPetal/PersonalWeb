extends Node2D

const ARENA_SIZE := Vector2(960.0, 540.0)
const TANK_SIZE := Vector2(42.0, 30.0)
const TANK_SPEED := 240.0
const BULLET_SPEED := 520.0

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
var server_tanks: Dictionary = {}
var server_projectiles: Array = []
var fire_requested := false

func _ready() -> void:
	_connect_to_server()
	queue_redraw()

func _process(delta: float) -> void:
	_move_player(delta)
	_update_bullets(delta)
	_poll_websocket()
	_send_input()
	queue_redraw()

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseMotion:
		player_angle = player_position.angle_to_point(event.position)
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
		if game_over:
			_restart()
		else:
			_shoot()
	if event is InputEventKey and event.pressed and not event.echo and event.keycode == KEY_R:
		_restart()

func _move_player(delta: float) -> void:
	if server_connected:
		return
	var direction := Input.get_vector("move_left", "move_right", "move_up", "move_down")
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
	targets = [Vector2(180, 150), Vector2(760, 145), Vector2(210, 410), Vector2(750, 390)]
	score = 0
	game_over = false

func _connect_to_server() -> void:
	var websocket_url := "ws://127.0.0.1:8080/tankgame-ws"
	if OS.has_feature("web"):
		var protocol: String = JavaScriptBridge.eval("location.protocol")
		var host: String = JavaScriptBridge.eval("location.host")
		websocket_url = ("wss://" if protocol == "https:" else "ws://") + host + "/tankgame-ws"
	var error := websocket.connect_to_url(websocket_url)
	if error != OK:
		websocket_status = "WebSocket unavailable (offline prototype still works)"

func _poll_websocket() -> void:
	websocket.poll()
	match websocket.get_ready_state():
		WebSocketPeer.STATE_OPEN:
			server_connected = true
			websocket_status = "Connected · Finding a battle..."
			if not queue_sent:
				websocket.send_text(JSON.stringify({"action": "queue", "playerName": "Pilot"}))
				queue_sent = true
			while websocket.get_available_packet_count() > 0:
				var message := websocket.get_packet().get_string_from_utf8()
				if message.length() > 0:
					_handle_server_message(message)
		WebSocketPeer.STATE_CONNECTING:
			websocket_status = "Connecting to Spring WebSocket..."
		WebSocketPeer.STATE_CLOSED:
			server_connected = false
			queue_sent = false
			if websocket_status.begins_with("Connected") or websocket_status.begins_with("Finding"):
				websocket_status = "Spring WebSocket disconnected"

func _send_input() -> void:
	if not server_connected or local_tank_id.is_empty():
		return
	var mouse_position := get_viewport().get_mouse_position()
	var input := {
		"up": Input.is_action_pressed("move_up"),
		"down": Input.is_action_pressed("move_down"),
		"left": Input.is_action_pressed("move_left"),
		"right": Input.is_action_pressed("move_right"),
		"shoot": fire_requested,
		"mouseX": mouse_position.x,
		"mouseY": mouse_position.y
	}
	websocket.send_text(JSON.stringify({"action": "input", "input": input}))
	fire_requested = false

func _handle_server_message(message: String) -> void:
	var payload = JSON.parse_string(message)
	if payload == null or not payload is Dictionary:
		return
	match payload.get("type", ""):
		"connected":
			websocket_status = "Connected · Finding a battle..."
		"joined":
			local_tank_id = str(payload.get("tankId", ""))
			websocket_status = "Queued · Waiting for another pilot..."
		"state":
			var game: Dictionary = payload.get("game", {})
			server_tanks = game.get("tanks", {})
			server_projectiles = game.get("projectiles", [])
			var game_status: String = str(game.get("status", "WAITING"))
			websocket_status = "Battle live · %d pilots" % server_tanks.size() if game_status == "PLAYING" else "Queued · Waiting for another pilot..."
			for tank_id in server_tanks:
				if str(tank_id) == local_tank_id:
					var tank: Dictionary = server_tanks[tank_id]
					player_position = Vector2(float(tank.get("x", 0.0)) + 20.0, float(tank.get("y", 0.0)) + 20.0)
					player_angle = float(tank.get("rotation", 0.0))
		"error":
			websocket_status = "Battle error: " + str(payload.get("message", "Unknown error"))

func _draw() -> void:
	draw_rect(Rect2(Vector2.ZERO, ARENA_SIZE), Color("101827"))
	draw_rect(Rect2(18, 82, ARENA_SIZE.x - 36, ARENA_SIZE.y - 100), Color("17243a"), true)
	for x in range(40, 960, 40):
		draw_line(Vector2(x, 100), Vector2(x, 520), Color("20304b"), 1.0)
	for y in range(100, 540, 40):
		draw_line(Vector2(20, y), Vector2(940, y), Color("20304b"), 1.0)

	var font := ThemeDB.fallback_font
	draw_string(font, Vector2(28, 34), "IRONBOUND ONLINE · ACTIVE DEVELOPMENT", HORIZONTAL_ALIGNMENT_LEFT, -1, 22, Color("e8eef8"))
	draw_string(font, Vector2(28, 62), "WASD / arrow keys to move · mouse or space to shoot", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Color("9fb2cf"))
	draw_string(font, Vector2(680, 34), "Pilots: %d" % server_tanks.size() if server_connected else "Score: %d" % score, HORIZONTAL_ALIGNMENT_LEFT, -1, 20, Color("ffd166"))
	draw_string(font, Vector2(28, 525), websocket_status, HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color("8bd5ca"))

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
		draw_string(font, Vector2(352, 260), "ARENA CLEAR", HORIZONTAL_ALIGNMENT_LEFT, -1, 28, Color("95e1d3"))
		draw_string(font, Vector2(315, 294), "Click or press R to play again", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Color("e8eef8"))

	if not server_connected:
		draw_set_transform(player_position, player_angle)
		draw_rect(Rect2(-TANK_SIZE.x / 2.0, -TANK_SIZE.y / 2.0, TANK_SIZE.x, TANK_SIZE.y), Color("4ecdc4"), true)
		draw_rect(Rect2(-6, -5, 32, 10), Color("95e1d3"), true)
		draw_circle(Vector2.ZERO, 9.0, Color("264653"))
		draw_set_transform(Vector2.ZERO, 0.0)

func _draw_server_state(font: Font) -> void:
	for projectile in server_projectiles:
		draw_circle(Vector2(float(projectile.get("x", 0.0)), float(projectile.get("y", 0.0))), 5.0, Color("ffd166"))
	for tank_id in server_tanks:
		var tank: Dictionary = server_tanks[tank_id]
		var tank_position := Vector2(float(tank.get("x", 0.0)) + 20.0, float(tank.get("y", 0.0)) + 20.0)
		var tank_color := Color(str(tank.get("color", "#4ecdc4")))
		draw_set_transform(tank_position, float(tank.get("rotation", 0.0)))
		draw_rect(Rect2(-20.0, -20.0, 40.0, 40.0), tank_color, true)
		draw_rect(Rect2(-6.0, -5.0, 32.0, 10.0), tank_color.lightened(0.35), true)
		draw_circle(Vector2.ZERO, 9.0, Color("264653"))
		draw_set_transform(Vector2.ZERO, 0.0)
		draw_string(font, tank_position + Vector2(-24.0, -28.0), str(tank.get("playerName", "Pilot")), HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color("e8eef8"))
		draw_string(font, tank_position + Vector2(-20.0, 38.0), "%d HP" % int(tank.get("health", 0)), HORIZONTAL_ALIGNMENT_LEFT, -1, 11, Color("95e1d3"))
