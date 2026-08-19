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

func _ready() -> void:
	_connect_to_server()
	queue_redraw()

func _process(delta: float) -> void:
	_move_player(delta)
	_update_bullets(delta)
	_poll_websocket()
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
	var direction := Input.get_vector("move_left", "move_right", "move_up", "move_down")
	player_position += direction * TANK_SPEED * delta
	player_position.x = clamp(player_position.x, 32.0, ARENA_SIZE.x - 32.0)
	player_position.y = clamp(player_position.y, 90.0, ARENA_SIZE.y - 32.0)

func _shoot() -> void:
	if game_over:
		return
	var muzzle := player_position + Vector2.RIGHT.rotated(player_angle) * 30.0
	bullets.append({"position": muzzle, "direction": Vector2.RIGHT.rotated(player_angle)})

func _update_bullets(delta: float) -> void:
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
			websocket_status = "Connected to Spring WebSocket"
			while websocket.get_available_packet_count() > 0:
				var message := websocket.get_packet().get_string_from_utf8()
				if message.length() > 0:
					websocket_status = "Connected: " + message
		WebSocketPeer.STATE_CONNECTING:
			websocket_status = "Connecting to Spring WebSocket..."
		WebSocketPeer.STATE_CLOSED:
			if websocket_status.begins_with("Connected"):
				websocket_status = "Spring WebSocket disconnected"

func _draw() -> void:
	draw_rect(Rect2(Vector2.ZERO, ARENA_SIZE), Color("101827"))
	draw_rect(Rect2(18, 82, ARENA_SIZE.x - 36, ARENA_SIZE.y - 100), Color("17243a"), true)
	for x in range(40, 960, 40):
		draw_line(Vector2(x, 100), Vector2(x, 520), Color("20304b"), 1.0)
	for y in range(100, 540, 40):
		draw_line(Vector2(20, y), Vector2(940, y), Color("20304b"), 1.0)

	var font := ThemeDB.fallback_font
	draw_string(font, Vector2(28, 34), "TANK GAME · GODOT WEB PROTOTYPE", HORIZONTAL_ALIGNMENT_LEFT, -1, 22, Color("e8eef8"))
	draw_string(font, Vector2(28, 62), "WASD / arrow keys to move · mouse or space to shoot", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Color("9fb2cf"))
	draw_string(font, Vector2(680, 34), "Score: %d" % score, HORIZONTAL_ALIGNMENT_LEFT, -1, 20, Color("ffd166"))
	draw_string(font, Vector2(28, 525), websocket_status, HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color("8bd5ca"))

	for target in targets:
		draw_circle(target, 22.0, Color("e76f51"))
		draw_circle(target, 12.0, Color("f4a261"))
		draw_circle(target, 4.0, Color("264653"))
	for bullet in bullets:
		draw_circle(bullet.position, 5.0, Color("ffd166"))
	if game_over:
		draw_rect(Rect2(260, 220, 440, 100), Color(0.05, 0.09, 0.15, 0.92), true)
		draw_string(font, Vector2(352, 260), "ARENA CLEAR", HORIZONTAL_ALIGNMENT_LEFT, -1, 28, Color("95e1d3"))
	draw_string(font, Vector2(315, 294), "Click or press R to play again", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Color("e8eef8"))

	draw_set_transform(player_position, player_angle)
	draw_rect(Rect2(-TANK_SIZE.x / 2.0, -TANK_SIZE.y / 2.0, TANK_SIZE.x, TANK_SIZE.y), Color("4ecdc4"), true)
	draw_rect(Rect2(-6, -5, 32, 10), Color("95e1d3"), true)
	draw_circle(Vector2.ZERO, 9.0, Color("264653"))
	draw_set_transform(Vector2.ZERO, 0.0)
