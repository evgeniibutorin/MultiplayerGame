
using NativeWebSocket;
using System;
using System.Text;
using UnityEngine;

public class GameNetworkManager : MonoBehaviour
{

    [Header("Player Manager")]

    [SerializeField]
    private PlayerManager playerManager;

    private WebSocket websocket;

    private PlayerData localPlayer;

    [Header("Chat Manager")]
    [SerializeField]
    private ChatManager chatManager;

    [Header("Login UI Controller")]
    [SerializeField]
    private LoginUIController loginUIController;

    public async void ConnectToServer(string playerName)
    {
        websocket = new WebSocket("ws://localhost:8080/game");

        websocket.OnOpen += () =>
        {
            Debug.Log("Connected to game server!");

            SendJoinMessage(playerName);
        };

        websocket.OnError += (error) =>
        {
            Debug.LogError("WebSocket error: " + error);
        };

        websocket.OnClose += (code) =>
        {
            Debug.Log("WebSocket closed: " + code);
        };

        websocket.OnMessage += (bytes) =>
        {
            string message = Encoding.UTF8.GetString(bytes);

            Debug.Log("Server message: " + message);

            HandleServerMessage(message);
        };

        try
        {
            await websocket.Connect();
        }
        catch (Exception e)
        {
            Debug.LogError("Connection failed: " + e.Message);
        }
    }


    private async void SendJoinMessage(string playerName)
    {
        JoinRequest request = new JoinRequest
        {
            type = "JOIN",
            name = playerName
        };

        string json = JsonUtility.ToJson(request);

        await websocket.SendText(json);

        Debug.Log("JOIN message sent: " + json);
    }

    private void HandleServerMessage(string json)
    {
        ServerMessage message =
            JsonUtility.FromJson<ServerMessage>(json);

        switch (message.type)
        {
            case "WELCOME":
                Debug.Log("Server welcomed us!");
                break;

            case "JOINED":
                HandleJoined(json);
                break;

            case "PLAYER_LIST":
                HandlePlayerList(json);
                break;

            case "PLAYER_JOINED":
                HandlePlayerJoined(json);
                break;

            case "PLAYER_LEFT":
                HandlePlayerLeft(json);
                break;

            case "PLAYER_MOVED":
                HandlePlayerMoved(json);
                break;

            case "CHAT_MESSAGE":
                chatManager.HandleChatMessage(json);
                break;

            default:
                Debug.Log("Unhandled message: " + message.type);
                break;
        }
    }

    private void HandlePlayerMoved(string json)
    {
        PlayerMovedMessage message =
            JsonUtility.FromJson<PlayerMovedMessage>(json);

        if (message.id == localPlayer.id)
        {
            localPlayer.x = message.x;
            localPlayer.y = message.y;
        }

        playerManager.MovePlayer(
            message.id,
            message.x,
            message.y
        );
    }

    public async void SendMoveMessageToServer(string direction)
    {
        if (websocket == null ||
            websocket.State != WebSocketState.Open)
        {
            return;
        }

        MoveRequest request = new MoveRequest
        {
            type = "MOVE",
            direction = direction
        };

        string json = JsonUtility.ToJson(request);

        await websocket.SendText(json);

        Debug.Log("Move request sent: " + direction);
    }

    private void HandlePlayerList(string json)
    {
        PlayerListMessage message =
            JsonUtility.FromJson<PlayerListMessage>(json);

        foreach (PlayerData player in message.players)
        {
            if (player.id == localPlayer.id)
            {
                continue;
            }

            CreateOtherPlayer(player);
        }
    }

    private void HandlePlayerJoined(string json)
    {
        PlayerJoinedMessage message =
            JsonUtility.FromJson<PlayerJoinedMessage>(json);

        Debug.Log("New player joined: " + message.player.name);

        CreateOtherPlayer(message.player);
    }

    private void CreateOtherPlayer(PlayerData player)
    {
        playerManager.CreatePlayer(player);
    }

    private void HandlePlayerLeft(string json)
    {
        PlayerLeftMessage message =
            JsonUtility.FromJson<PlayerLeftMessage>(json);

        playerManager.RemovePlayer(message.id);
    }

    private void HandleJoined(string json)
    {
        JoinedMessage message =
            JsonUtility.FromJson<JoinedMessage>(json);

        localPlayer = new PlayerData
        {
            id = message.id,
            name = message.name,
            color = message.color,
            x = message.x,
            y = message.y
        };

        Debug.Log("Player registered!");
        Debug.Log("ID: " + localPlayer.id);
        Debug.Log("Name: " + localPlayer.name);
        Debug.Log("Color: " + localPlayer.color);
        Debug.Log("Position: " + localPlayer.x + ", " + localPlayer.y);

        playerManager.CreatePlayer(localPlayer);

        loginUIController.ShowGameScreen();
    }

    public async void SendChatMessageToServer(string message)
    {
        if (websocket == null ||
            websocket.State != WebSocketState.Open)
        {
            return;
        }

        ChatRequest request = new ChatRequest
        {
            type = "CHAT",
            message = message
        };

        string json = JsonUtility.ToJson(request);

        await websocket.SendText(json);

        Debug.Log("Chat message sent: " + json);
    }

    private async void OnApplicationQuit()
    {
        if (websocket != null)
        {
            await websocket.Close();
        }
    }

    [Serializable]
private class PlayerListMessage
{
    public string type;
    public PlayerData[] players;
}

    [Serializable]
    private class MoveRequest
    {
        public string type;
        public string direction;
    }

    [Serializable]
    private class JoinRequest
    {
        public string type;
        public string name;
    }

    [Serializable]
    private class ServerMessage
    {
        public string type;
    }

    [Serializable]
    private class JoinedMessage
    {
        public string type;
        public string id;
        public string name;
        public string color;
        public float x;
        public float y;
    }

    [Serializable]
    private class PlayerMovedMessage
    {
        public string type;
        public string id;
        public float x;
        public float y;
    }

    [Serializable]
    private class PlayerJoinedMessage
    {
        public string type;
        public PlayerData player;
    }

    [Serializable]
    private class PlayerLeftMessage
    {
        public string type;
        public string id;
    }

    [Serializable]
    private class ChatRequest
    {
        public string type;
        public string message;
    }
}