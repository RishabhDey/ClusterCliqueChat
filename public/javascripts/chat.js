const userStatus = {};
let socket, chatBox, userTable;

function initChat(roomId, jwt) {
  socket = new WebSocket(`ws://${window.location.host}/ws/chat/${roomId}?token=${jwt}`);
  console.log(jwt);
  chatBox = document.getElementById("chatBox");
  userTable = document.getElementById("userStatus");


  socket.onopen = () => {
    console.log("WebSocket is connected");
  };

  socket.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    console.log("Message Recieved", msg)
    if (Array.isArray(msg)) {
      msg.forEach(handleMessage);
    } else {
      handleMessage(msg);
    }
  };

  socket.onerror = (err) => {
    window.location.href = "/";
  };

  socket.onclose = () => {
    console.log("WebSocket connection closed");
  };

  chatBox.addEventListener('scroll', handleScroll);

}




function handleMessage(msg) {
    switch (msg.typ) {
        case "chat":
            appendMessage(`[${msg.user.userId}]: ${msg.chatMessage}`);
            break;
        case "post":
            appendMessage(`[${msg.user.userId}]: ${msg.postMessage}`);
            appendImage(msg.post.imgURL);
            break;
        case "userJoined":
            userStatus[msg.userJoined.userId] = msg.userJoined.status;
            updateUserStatus(msg.userJoined.userId, msg.userJoined.status);
            break;
        case "userLeft":
            userStatus[msg.userLeft.userId] = msg.userLeft.status;
            updateUserStatus(msg.userLeft.userId, msg.userLeft.status);
            break;
        case "chatRoom":
            msg.members.forEach(u => {
                userStatus[u.userId] = u.status;
                updateUserStatus(u.userId, u.status);
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

function sendChatMessage(currentUser) {
    const input = document.getElementById("chatInput");
    const text = input.value.trim();
    if (text) {
        const payload = {
            typ: "sendChat",
            sendChatMessage: text
        };
        socket.send(JSON.stringify(payload));
        input.value = "";
    }
}

function showPostInput() {
    document.getElementById("postInputContainer").style.display = "block";
}

function sendPostMessage() {
    const input = document.getElementById("PostInput");
    const url = input.value.trim();
    if (url) {
        const payload = {
            typ: "sendPost",
            sendPostMessage: url
        };
        socket.send(JSON.stringify(payload));
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

const chatInput = document.getElementById("chatInput");

chatInput.addEventListener("keydown", function(event) {
  if (event.key === "Enter") {
    event.preventDefault();
    sendChatMessage();
  }
});
