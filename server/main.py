from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="CallYar Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

users = {}


@app.get("/")
async def root():
    return {
        "app": "CallYar",
        "status": "online"
    }


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "users": len(users),
        "online_users": list(users.keys())
    }


@app.get("/users")
async def get_users():
    return {
        "users": list(users.keys())
    }


async def send_users():
    message = {
        "type": "users",
        "users": list(users.keys())
    }

    disconnected = []

    for username, websocket in list(users.items()):
        try:
            await websocket.send_json(message)
        except Exception:
            disconnected.append(username)

    for username in disconnected:
        if username in users:
            del users[username]


async def send_to_user(target, message):
    target_socket = users.get(target)

    if target_socket is None:
        return False

    try:
        await target_socket.send_json(message)
        return True

    except Exception:
        if users.get(target) is target_socket:
            del users[target]

        return False


@app.websocket("/ws/{username}")
async def websocket_endpoint(
    websocket: WebSocket,
    username: str
):
    await websocket.accept()

    username = username.strip()

    if not username:
        await websocket.close()
        return

    old_socket = users.get(username)

    users[username] = websocket

    if old_socket is not None and old_socket is not websocket:
        try:
            await old_socket.close()
        except Exception:
            pass

    print(f"[ONLINE] {username}")
    print(f"[USERS] {list(users.keys())}")

    await send_users()

    try:
        while True:

            message = await websocket.receive_json()

            message_type = message.get("type")
            target = message.get("target")

            print(
                f"[MESSAGE] from={username} "
                f"type={message_type} "
                f"target={target}"
            )

            if not target:
                print("[ERROR] target is missing")
                continue

            target = str(target).strip()

            if not target:
                print("[ERROR] target is empty")
                continue

            if target not in users:
                print(
                    f"[ERROR] target '{target}' is not online"
                )

                await websocket.send_json({
                    "type": "error",
                    "from": "server",
                    "data": {
                        "message": "کاربر مقصد آنلاین نیست",
                        "target": target
                    }
                })

                continue

            outgoing = {
                "type": message_type,
                "from": username,
                "data": message.get("data")
            }

            success = await send_to_user(
                target,
                outgoing
            )

            if success:
                print(
                    f"[SENT] {message_type} "
                    f"{username} -> {target}"
                )
            else:
                print(
                    f"[FAILED] {username} -> {target}"
                )

    except WebSocketDisconnect:
        print(f"[OFFLINE] {username}")

    except Exception as e:
        print(
            f"[ERROR] {username}: {e}"
        )

    finally:

        if users.get(username) is websocket:

            del users[username]

            print(f"[REMOVED] {username}")
            print(f"[USERS] {list(users.keys())}")

            await send_users()