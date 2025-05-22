const userStatus = {};
let socket, chatBox, userTable;

function initChat(userId, roomId) {
  const socket = new WebSocket(`ws://${window.location.host}/ws/chat/${userId}/${roomId}`);

  const chatBox = document.getElementById("chatBox");
  const userTable = document.getElementById("userStatus");

  socket.onopen = () => {
    console.log("WebSocket is connected");
  };

  socket.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    if (Array.isArray(msg)) {
      msg.forEach(handleMessage);
    } else {
      handleMessage(msg);
    }
  };

  socket.onerror = (err) => {
    console.error("WebSocket error", err);
  };

  socket.onclose = () => {
    console.log("WebSocket connection closed");
  };

}


function handleMessage(msg) {
    switch (msg.type) {
        case "chat":
            appendMessage(`[${msg.user.userId}]: ${msg.message}`);
            break;
        case "post":
            appendMessage(`[${msg.user.userId}]: ${msg.message}`);
            appendImage(msg.post.imgURL);
            break;
        case "UserJoined":
        case "UserLeft":
            userStatus[msg.user.userId] = msg.user.status.status;
            updateUserStatus(msg.user.userId, msg.user.status.status);
            break;
        case "ChatRoom":
            msg.users.forEach(u => {
                userStatus[u.userId] = u.status.status;
                updateUserStatus(u.userId, u.status.status);
            });
            msg.messages.forEach(handleMessage);
            break;
    }
}

function appendMessage(text) {
    const p = document.createElement("p");
    p.innerText = text;
    chatBox.appendChild(p);
    chatBox.scrollTop = chatBox.scrollHeight;
}

function appendImage(url) {
    const img = document.createElement("img");
    img.src = url;
    img.style.maxWidth = "200px";
    chatBox.appendChild(img);
}

function sendChatMessage() {
    const input = document.getElementById("chatInput");
    const text = input.value.trim();
    if (text) {
        socket.send(JSON.stringify({ type: "chat", message: text }));
        input.value = "";
    }
}

function showPostInput() {
    document.getElementById("postInputContainer").style.display = "block";
}

function sendPostMessage() {
    const input = document.getElementById("PostInput");
    const text = input.value.trim();
    if (text) {
        socket.send(JSON.stringify({ type: "post", post: { imgURL: text } }));
        input.value = "";
    }
    document.getElementById("postInputContainer").style.display = "none";
}

function addUserToTable(userId, status) {
    const tr = document.createElement("tr");
    tr.setAttribute("data-user-id", userId);

    const tdUser = document.createElement("td");
    tdUser.innerText = userId;

    const tdStatus = document.createElement("td");
    tdStatus.innerText = status;

    tr.appendChild(tdUser);
    tr.appendChild(tdStatus);
    userTable.appendChild(tr);
}

function updateUserStatus(userId, status) {
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);
    if (row) {
        row.children[1].innerText = status;
    } else {
        addUserToTable(userId, status);
    }
}
