<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { over } from 'stompjs';
import  SockJS  from "sockjs-client/dist/sockjs"

let stompClient = null;
let privateChats = ref(new Map())
let publicChats = ref([])
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
    stompClient.subscribe('/chatroom/public', onMessageReceived);
    stompClient.subscribe('/user/'+userData.value.username+'/private', onPrivateMessage);
    userJoin();
}


const userJoin=()=>{
    var chatMessage = {
        senderName: userData.value.username,  
        status:"JOIN"
    };
    stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
}

const onMessageReceived = (payload)=>{
    var payloadData = JSON.parse(payload.body);
    switch(payloadData.status){
        case "JOIN":
            if(!privateChats.value.get(payloadData.senderName)){
                privateChats.value.set(payloadData.senderName,[]);
                // privateChats.value = new Map(privateChats)
            }
            break;
        case "MESSAGE":
            publicChats.push(payloadData);
            publicChats.value = publicChats
            break;
    }
}

const onPrivateMessage = (payload)=>{
    console.log(payload);
    var payloadData = JSON.parse(payload.body);
    if(privateChats.value.get(payloadData.senderName)){
        privateChats.value.get(payloadData.senderName).push(payloadData);
        // privateChats.value = new Map(privateChats);
    }else{
        let list =[];
        list.push(payloadData);
        privateChats.value.set(payloadData.senderName,list);
        // privateChats.value = new Map(privateChats)
    }
}

const handleMessage =(event)=>{
    const {value}=event.target;
    userData.value.message = value;
}

const sendValue=()=>{
    if (stompClient) {
        var chatMessage = {
            senderName: userData.value.username,
            message: userData.value.message,
            status:"MESSAGE"
        };
        console.log(chatMessage);
        stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
        userData.value.message = "";
    }
}

const sendPrivateValue=()=>{
    if (stompClient) {
        var chatMessage = {
            senderName: userData.value.username,
            receiverName:tab,
            message: userData.value.message,
            status:"MESSAGE"
        };
        if(userData.value.username !== tab){
            privateChats.value.get(tab).push(chatMessage);
            // privateChats.value = new Map(privateChats);
        }
        stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
        userData.value.message = "";
    }
}

const handleUsername=(event)=>{
    const {value}=event.target;
    userData.value.username = value;
}

const registerUser=()=>{
    console.log("register")
    connect();
    
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
        @click="handleUsername"
        margin="normal"
        />
        <button type="button" @click="registerUser">
            进入聊天APP
        </button> 
</div>

<div class="chat-box" v-if="userData.connected==true">

    <div class="member-list">
        <ul>
            <li @click="tab='CHATROOM'" class="member">Chatroom</li>
            <!-- <li @click="tab.value='CHATROOM'" class="member">Chatroom</li> -->
            <li v-for="(name, index) in privateChats" @click="tab=name" class="member" :key="index">{{name}}</li>
        </ul>
    </div>

</div>

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
</style>