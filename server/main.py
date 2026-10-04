from fastapi import FastAPI, WebSocket, WebSocketDisconnect

app = FastAPI(title="CallYar Server")
users = {}

@app.get("/")
async def root():
    return {"app":"CallYar","status":"online","service":"signaling"}

@app.get("/health")
async def health():
    return {"status":"ok"}

@app.get("/users")
async def get_users():
    return {"users":list(users.keys())}

async def broadcast_users():
    msg={"type":"users","users":list(users.keys())}
    for name, ws in list(users.items()):
        try: await ws.send_json(msg)
        except Exception: users.pop(name,None)

@app.websocket("/ws/{username}")
async def websocket_endpoint(websocket: WebSocket, username: str):
    username=username.strip()
    if not username:
        await websocket.close(code=1008)
        return
    await websocket.accept()
    users[username]=websocket
    await websocket.send_json({"type":"connected","username":username})
    await broadcast_users()
    try:
        while True:
            data=await websocket.receive_json()
            target=data.get("target")
            if not target: continue
            target_socket=users.get(target)
            if not target_socket:
                await websocket.send_json({"type":"error","message":"کاربر آنلاین نیست"})
                continue
            await target_socket.send_json({
                "type":data.get("type"),
                "from":username,
                "target":target,
                "data":data.get("data")
            })
    except (WebSocketDisconnect, Exception):
        if users.get(username) is websocket:
            users.pop(username,None)
        await broadcast_users()
