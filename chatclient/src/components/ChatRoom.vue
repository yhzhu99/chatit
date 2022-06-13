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
        status:"JOIN"
    };

    for (const [key, value] of friendsList.value.entries()) {
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
            friendsList.value.set(payloadData.senderName, 1);
            if(!privateChats.get(payloadData.senderName)){
                privateChats.set(payloadData.senderName,[]);
            }
            break;
        case "MESSAGE":
            console.log(publicChats.value)
            publicChats.value.push(payloadData);
            break;
    }
       
}

const onPrivateMessageReceived = (payload)=>{
    // console.log(payload);
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

const sendPrivateMessage=()=>{
    if (stompClient) {
        var chatMessage = {
            senderName: userData.value.username,
            receiverName:tab.value,
            message: userData.value.message,
            status:"MESSAGE"
        };
        if(userData.value.username !== tab.value){
            privateChats.set(tab.value, [...privateChats.get(tab.value), chatMessage]);
        }
        stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
        userData.value.message = "";
    }
}

const changeTab=(v)=>{
    tab.value=v;
    console.log(tab.value)
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
        margin="normal"
        />
        <button type="button" @click="registerUser()">
            进入聊天APP
        </button> 
</div>

<div class="chat-box" v-if="userData.connected==true">

    <div class="member-list">
        <ul>
            <li @click="changeTab('CHATROOM')" class="member">Chatroom</li>
            <!-- <li @click="tab.value='CHATROOM'" class="member">Chatroom</li> -->
            <li v-for="(name, index) in friendsList.keys()" @click="changeTab(name)" class="member" :key="index">{{name}}</li>
        </ul>
    </div>

    <div class="chat-content">
        <ul class="chat-messages" v-if="tab==='CHATROOM'">
                <li v-for="(chat, index) in publicChats" class="message" :key="index">
                    <div class="avatar">{{chat.senderName}}</div>
                    <div class="message-data">{{chat.message}}</div>
                </li>
        </ul>
        <ul class="chat-messages" v-else>
                <li v-for="(chat, index) in privateChats.get(tab)" class="message" :key="index">
                    <div class="avatar">{{chat.senderName}}</div>
                    <div class="message-data">{{chat.message}}</div>
                </li>
        </ul>
        <div class="send-message">
            <input type="text" class="input-message" placeholder="输入消息" v-model="userData.message"/> 
            <button type="button" class="send-button" @click="sendPublicMessage()" v-if="tab==='CHATROOM'">发送</button>
            <button type="button" class="send-button" @click="sendPrivateMessage()" v-else>发送</button>
        </div>
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