<script setup>
import { ref, unref, reactive, onMounted, onUnmounted } from 'vue'
import { useStorage } from '@vueuse/core'
import { over } from 'stompjs';
import  SockJS  from "sockjs-client/dist/sockjs"

let stompClient = null;
let privateChats = reactive(new Map())
// const privateChats = useStorage('privateChats', new Map())
// let publicChats = ref([])
const publicChats = useStorage('publicChats', [])
const friendsList = useStorage('friendsList', new Map())

let tab = ref("CHATROOM")
let userData = ref({
    username: '',
    receiverName: '',
    connected: false,
    message: ''
})

const onError = (err) => {
    console.log(err);    
}

const connect =()=>{
    let sock = new SockJS('http://localhost:8080/ws');
    stompClient = over(sock);
    stompClient.connect({}, onConnected, onError);
}

const onConnected = () => {
    userData.value.connected=true;
    console.log("🚀 ~ file: ChatRoom.vue ~ line 30 ~ onConnected ~ userData", userData.value)
    stompClient.subscribe('/chatroom/public', onPublicMessageReceived);
    stompClient.subscribe('/user/'+userData.value.username+'/private', onPrivateMessageReceived);
    userJoin();
}


const userJoin=()=>{
    var chatMessage = {
        senderName: userData.value.username,  
        messageType: "text",
        status:"JOIN"
    };

    // 刚进入的时候，更新我的在线状态
    friendsList.value.set(userData.value.username, "[在线]")

    // 初始化privateChats的map
    for (const [key, value] of friendsList.value.entries()) {
        // if ( friendsList.value.get(key) == '[在线]' ) {
        //     continue
        // }
        // else if (friendsList.value.get(key) == '[未知]' && key == userData.value.username) {
        //     friendsList.value.set(key, "[在线]")
        // } else{ 
        //     friendsList.value.set(key, "[未知]")
        // }
        // console.log(key, value);
        if(!privateChats.get(key)){
            privateChats.set(key,[]);
        }
    }

    stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
}

const onPublicMessageReceived = (payload)=>{
    var payloadData = JSON.parse(payload.body);
    switch(payloadData.status){
        case "JOIN":
            if(!privateChats.get(payloadData.senderName)){
                privateChats.set(payloadData.senderName,[]);
            }
            // 当别人上线时，更新他的在线状态
            friendsList.value.set(payloadData.senderName, "[在线]")
            break;
        case "MESSAGE":
            console.log(publicChats.value)
            publicChats.value.push(payloadData);
            // 当别人群发消息时，表明他在线 (不需要做这个处理，因为上线时即通知了其在线)
            // friendsList.value.set(payloadData.senderName, "[在线]")
            break;
        case "LEAVE":
            // 当别人下线时，更新他的离线状态
            friendsList.value.set(payloadData.senderName, "[离线]")
            break;
    }
       
}

const onPrivateMessageReceived = (payload)=>{
    // console.log(payload);
    // 收到私发消息时，表明他在线 (不需要做这个处理，因为上线时即通知了其在线)
    // friendsList.value.set(payloadData.senderName, "[在线]")
    var payloadData = JSON.parse(payload.body);
    if(privateChats.get(payloadData.senderName)){
        privateChats.set(payloadData.senderName, [...privateChats.get(payloadData.senderName), payloadData]);
    }else{
        let list =[];
        list.push(payloadData);
        privateChats.set(payloadData.senderName, list);
    }
}

const sendPublicMessage=()=>{
    const f = document.getElementById("fileInput").files[0];
    if (stompClient) {
        if (userData.value.message){
            var chatMessage = {
                senderName: userData.value.username,
                message: userData.value.message,
                messageType: "text",
                status:"MESSAGE"
            };
            console.log(chatMessage);
            stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
            userData.value.message = "";
        }
        if (!f){
            console.log("🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀")
        } else if (f.type=="image/png") {
            console.log("image!!!!")
            console.log("🚀1111🚀111🚀111🚀1")
            const reader = new FileReader();
            reader.onload = function(evt) { 
                // const metadata = `name: ${f.name}, type: ${f.type}, size: ${f.size}, contents:`;
                const contents = evt.target.result;
                // console.log(metadata, contents);f
                // document.getElementById("image").src = contents;
                var chatMessage = {
                    senderName: userData.value.username,
                    message: contents,
                    messageType: "image",
                    messageName: f.name,
                    status:"MESSAGE"
                };
                console.log("sent!!!!!!!!!!!!!!!!!!!", chatMessage.messageType);
                stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
            };
            reader.readAsDataURL(f);
        } else if (f.type=="video/mp4") {
            const reader = new FileReader();
            reader.onload = function(evt) { 
                const contents = evt.target.result;
                var chatMessage = {
                    senderName: userData.value.username,
                    message: contents,
                    messageType: "video",
                    messageName: f.name,
                    status:"MESSAGE"
                };
                stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
            };
            reader.readAsDataURL(f);
        } else if (f.type=="audio/mpeg") {
            const reader = new FileReader();
            reader.onload = function(evt) { 
                const contents = evt.target.result;
                var chatMessage = {
                    senderName: userData.value.username,
                    message: contents,
                    messageType: "audio",
                    messageName: f.name,
                    status:"MESSAGE"
                };
                stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
            };
            reader.readAsDataURL(f);
        } else {
            console.log("🚀🚀🚀file!!!!")
            const reader = new FileReader();
            reader.onload = function(evt) { 
                const contents = evt.target.result;
                var chatMessage = {
                    senderName: userData.value.username,
                    message: contents,
                    messageType: "file",
                    messageName: f.name,
                    status:"MESSAGE"
                };
                stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
            };
            reader.readAsDataURL(f);
        }
    }
}

const sendPrivateMessage=()=>{
    const f = document.getElementById("fileInput").files[0];
    if (stompClient) {
        if (userData.value.message){
            var chatMessage = {
                senderName: userData.value.username,
                receiverName: tab.value,
                message: userData.value.message,
                messageType: "text",
                status:"MESSAGE"
            };
            if(userData.value.username !== tab.value){
                privateChats.set(tab.value, [...privateChats.get(tab.value), chatMessage]);
            }
            stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
            userData.value.message = "";
        }
        if (!f){
            console.log("🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀🚀")
        } else if (f.type=="image/png") {
            const reader = new FileReader();
            reader.onload = function(evt) { 
                const contents = evt.target.result;
                var chatMessage = {
                    senderName: userData.value.username,
                    receiverName: tab.value,
                    message: contents,
                    messageType: "image",
                    messageName: f.name,
                    status:"MESSAGE"
                };
                if(userData.value.username !== tab.value){
                    privateChats.set(tab.value, [...privateChats.get(tab.value), chatMessage]);
                }
                stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
            };
            reader.readAsDataURL(f);
        } else if (f.type=="video/mp4") {
            const reader = new FileReader();
            reader.onload = function(evt) { 
                const contents = evt.target.result;
                var chatMessage = {
                    senderName: userData.value.username,
                    receiverName: tab.value,
                    message: contents,
                    messageType: "video",
                    messageName: f.name,
                    status:"MESSAGE"
                };
                if(userData.value.username !== tab.value){
                    privateChats.set(tab.value, [...privateChats.get(tab.value), chatMessage]);
                }
                stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
            };
            reader.readAsDataURL(f);
        } else if (f.type=="audio/mpeg") {
            const reader = new FileReader();
            reader.onload = function(evt) { 
                const contents = evt.target.result;
                var chatMessage = {
                    senderName: userData.value.username,
                    receiverName: tab.value,
                    message: contents,
                    messageType: "audio",
                    messageName: f.name,
                    status:"MESSAGE"
                };
                if(userData.value.username !== tab.value){
                    privateChats.set(tab.value, [...privateChats.get(tab.value), chatMessage]);
                }
                stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
            };
            reader.readAsDataURL(f);
        } else {
            const reader = new FileReader();
            reader.onload = function(evt) { 
                const contents = evt.target.result;
                var chatMessage = {
                    senderName: userData.value.username,
                    receiverName: tab.value,
                    message: contents,
                    messageType: "file",
                    messageName: f.name,
                    status:"MESSAGE"
                };
                if(userData.value.username !== tab.value){
                    privateChats.set(tab.value, [...privateChats.get(tab.value), chatMessage]);
                }
                stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
            };
            reader.readAsDataURL(f);
        }
    }
}

const downloadFile = (content, name)=>{
    const url = window.URL.createObjectURL(new Blob([JSON.stringify(content)]));
    const fileLink = document.createElement('a');
    fileLink.href = url;
    fileLink.setAttribute('download', name); //or any other extension
    document.body.appendChild(fileLink);
    fileLink.click();
}

const changeTab=(v)=>{
    tab.value=v;
    console.log(tab.value)
}

const registerUser=()=>{
    console.log("register")
    connect();
}

const leaveChat=()=>{
    console.log("leave")
    if (stompClient) {
        var chatMessage = {
            senderName: userData.value.username,
            messageType: "text",
            status:"LEAVE"
        };
        stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
        stompClient.disconnect();
        userData.value.connected=false;
    }
}

</script>

<template>

<h1>{{userData.username}} 在这里开启聊天~</h1>

<div class="register" v-if="userData.connected==false">
    <input
        id="user-name"
        placeholder="输入用户名"
        name="userName"
        v-model="userData.username"
        margin="normal"
        />
        <button type="button" @click="registerUser()">
            进入聊天APP
        </button> 
</div>

<div class="chat-box" v-if="userData.connected==true">
    <div class="member-list">
        <ul>
            <li @click="changeTab('CHATROOM')" class="member">群聊<div v-if="'CHATROOM'==tab">🍉</div></li>
            <!-- <li @click="tab.value='CHATROOM'" class="member">Chatroom</li> -->
            <li v-for="(name, index) in friendsList.keys()" @click="changeTab(name)" class="member" :key="index">{{name}} {{friendsList.get(name)}} <div v-if="name==tab">🍉</div> </li>
        </ul>
        <button type="button" class="leave-button" @click="leaveChat()">退出</button>
    </div>

    <div class="chat-content">
        <ul class="chat-messages" v-if="tab==='CHATROOM'">
                <li v-for="(chat, index) in publicChats" class="message" :key="index">
                    <div class="avatar">{{chat.senderName}}</div>
                    <div class="message-data" v-if="chat.messageType=='text'">{{chat.message}}</div>
                    <div class="message-data" v-if="chat.messageType=='image'">
                        <div class="demo-image__preview">
                            <el-image
                            style="width: 50px; height: 50px"
                            :initial-index="0"
                            :src="chat.message"
                            :preview-src-list="[chat.message]"
                            :alt="chat.messageName"
                            fit="cover"
                            />
                        </div>
                    </div>
                    <div class="message-data" v-if="chat.messageType=='file'">
                        <el-button size="large" round @click="downloadFile(chat.message, chat.messageName)">下载 {{chat.messageName}}</el-button>
                    </div>
                    <div class="message-data" v-if="chat.messageType=='video'">
                        <video alt="chat.messageName" width="150" height="150" controls>
                            <source :src="chat.message" type="video/mp4" />
                        </video>
                    </div>
                    <div class="message-data" v-if="chat.messageType=='audio'">
                        <audio alt="chat.messageName" controls>
                            <source :src="chat.message" type="audio/mpeg" />
                        </audio>
                    </div>
                </li>
        </ul>
        <ul class="chat-messages" v-else>
                <li v-for="(chat, index) in privateChats.get(tab)" class="message" :key="index">
                    <div class="avatar">{{chat.senderName}}</div>
                    <div class="message-data" v-if="chat.messageType=='text'">{{chat.message}}</div>
                    <div class="message-data" v-if="chat.messageType=='image'">
                        <div class="demo-image__preview">
                            <el-image
                            style="width: 50px; height: 50px"
                            :initial-index="0"
                            :src="chat.message"
                            :preview-src-list="[chat.message]"
                            :alt="chat.messageName"
                            fit="cover"
                            />
                        </div>
                    </div>
                    <div class="message-data" v-if="chat.messageType=='file'">
                        <el-button size="large" round @click="downloadFile(chat.message, chat.messageName)">下载 {{chat.messageName}}</el-button>
                    </div>
                    <div class="message-data" v-if="chat.messageType=='video'">
                        <video alt="chat.messageName" width="150" height="150" controls>
                            <source :src="chat.message" type="video/mp4" />
                        </video>
                    </div>
                    <div class="message-data" v-if="chat.messageType=='audio'">
                        <audio alt="chat.messageName" controls>
                            <source :src="chat.message" type="audio/mpeg" />
                        </audio>
                    </div>
                </li>
        </ul>
        <div class="send-message">
            <input type="text" class="input-message" placeholder="输入消息" v-model="userData.message"/>
            <input type="file" id="fileInput">
            <button type="button" class="send-button" @click="sendPublicMessage()" v-if="tab==='CHATROOM'">发送</button>
            <button type="button" class="send-button" @click="sendPrivateMessage()" v-else>发送</button>
        </div>
    </div>
</div>

<!-- <input type="file" id="fileInput">
<button type="button" class="send-button" @click="sendPublicMessage()">发送</button>
<img src="" height="200" id="image" alt="Image preview..."> -->

</template>

<style>
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

code {
  font-family: source-code-pro, Menlo, Monaco, Consolas, 'Courier New',
    monospace;
}

input {
  padding: 10px;
  font-size: 1.2em;
}
button{
  border: none;
  padding: 10px;
  background: green;
  color:#fff;
  font-size: 1.2em;
  font-weight: bold;
}


.container{
  position: relative;
}

.register{
  /* position: fixed; */
  padding:30px;
  box-shadow:0 2.8px 2.2px rgba(0, 0, 0, 0.034),0 6.7px 5.3px rgba(0, 0, 0, 0.048),0 12.5px 10px rgba(0, 0, 0, 0.06),0 22.3px 17.9px rgba(0, 0, 0, 0.072),0 41.8px 33.4px rgba(0, 0, 0, 0.086),0 100px 80px rgba(0, 0, 0, 0.12);
  top:35%;
  left:32%;
  display: flex;
  flex-direction: row;
}
.chat-box{
  box-shadow:0 2.8px 2.2px rgba(0, 0, 0, 0.034),0 6.7px 5.3px rgba(0, 0, 0, 0.048),0 12.5px 10px rgba(0, 0, 0, 0.06),0 22.3px 17.9px rgba(0, 0, 0, 0.072),0 41.8px 33.4px rgba(0, 0, 0, 0.086),0 100px 80px rgba(0, 0, 0, 0.12);
  margin:40px 50px;
  height: 600px;
  padding: 10px;
  display: flex;
  flex-direction: row;
}

.member-list{
  width: 20%;
}

.chat-content{
  width:80%;
  margin-left: 10px;
}

.chat-messages{
  height: 80%;
  border: 1px solid #000;
}

.send-message{
  width: 100%;
  display: flex;
  flex-direction: row;
}

.input-message{
  width:90%;
  border-radius: 50px;
}

ul {
  padding: 0;
  list-style-type: none;
}
.send-button{
  width:10%;
  border-radius: 50px;
  margin-left: 5px;
  cursor: pointer;
}
.leave-button{
    background-color: #008CBA; /* Green */
    border: none;
    color: white;
    padding: 15px 32px;
    text-align: center;
    text-decoration: none;
    display: inline-block;
    font-size: 16px;
    border-radius: 50px;
    cursor: pointer;
}
.member{
  padding: 10px;
  background: #eee;
  border:#000;
  cursor: pointer;
  margin: 5px 2px;
  box-shadow: 0 8px 8px -4px lightblue;
}
.member.active{
  background: blueviolet;
  color:#fff;
}
.member:hover{
  background: grey;
  color:#fff;
}

.avatar{
  background-color: cornflowerblue;
  padding: 3px 5px;
  border-radius: 5px;
  color:#fff;
}
.avatar.self{
  color:#000;
  background-color: greenyellow;
}
.message{
  padding:5px;
  width: auto;
  display: flex;
  flex-direction: row;
  box-shadow: 0 3px 10px rgb(0 0 0 / 0.2);
  margin: 5px 10px;
}
.message-data{
  padding:5px;
}
.message.self{
  justify-content: end;
}

.demo-image__error .image-slot {
  font-size: 30px;
}
.demo-image__error .image-slot .el-icon {
  font-size: 30px;
}
.demo-image__error .el-image {
  width: 100%;
  height: 200px;
}
</style>