<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { over } from 'stompjs';
import { SockJS } from "sockjs-client/dist/sockjs"


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
    let Sock = new SockJS('http://localhost:8080/ws');
    stompClient = over(Sock);
    stompClient.connect({}, onConnected, onError);
}

const onConnected = () => {
    userData.value.connected=true;
    stompClient.subscribe('/chatroom/public', onMessageReceived);
    stompClient.subscribe('/user/'+userData.username+'/private', onPrivateMessage);
    userJoin();
}


const userJoin=()=>{
    var chatMessage = {
        senderName: userData.username,
        status:"JOIN"
    };
    stompClient.send("/app/message", {}, JSON.stringify(chatMessage));
}

const onMessageReceived = (payload)=>{
    var payloadData = JSON.parse(payload.body);
    switch(payloadData.status){
        case "JOIN":
            if(!privateChats.get(payloadData.senderName)){
                privateChats.set(payloadData.senderName,[]);
                privateChats.value = new Map(privateChats)
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
    if(privateChats.get(payloadData.senderName)){
        privateChats.get(payloadData.senderName).push(payloadData);
        privateChats.value = new Map(privateChats);
    }else{
        let list =[];
        list.push(payloadData);
        privateChats.set(payloadData.senderName,list);
        privateChats.value = new Map(privateChats)
    }
}

const handleMessage =(event)=>{
    const {value}=event.target;
    userData.value.message = value;
}

const sendValue=()=>{
    if (stompClient) {
        var chatMessage = {
            senderName: userData.username,
            message: userData.message,
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
            senderName: userData.username,
            receiverName:tab,
            message: userData.message,
            status:"MESSAGE"
        };
        if(userData.username !== tab){
            privateChats.get(tab).push(chatMessage);
            privateChats.value = new Map(privateChats);
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
    connect();
}
</script>

<template>

<h1>{{userData.username}} 在这里开启聊天~</h1>

</template>
