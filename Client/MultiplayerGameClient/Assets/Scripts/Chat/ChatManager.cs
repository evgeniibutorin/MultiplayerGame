using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class ChatManager : MonoBehaviour
{
    [Header("Chat UI")]

    [SerializeField]
    private TMP_InputField chatInputField;

    [SerializeField]
    private TextMeshProUGUI chatMessagesText;

    [SerializeField]
    private Button chatSendButton;

    [Header("Chat Bubble")]

    [SerializeField]
    private GameObject chatBubblePrefab;

    [SerializeField]
    private Canvas chatCanvas;

    [Header("Managers")]

    [SerializeField]
    private PlayerManager playerManager;

    [SerializeField]
    private GameNetworkManager networkManager;

    private void Start()
    {
        chatSendButton.onClick.AddListener(SendChatMessage);
        chatInputField.onSubmit.AddListener(OnChatMessageSubmitted);
    }

    private void SendChatMessage()
    {
        string message = chatInputField.text.Trim();

        if (string.IsNullOrEmpty(message))
        {
            return;
        }

        networkManager.SendChatMessageToServer(message);

        chatInputField.text = "";
    }
    public void HandleChatMessage(string json)
    {
        ChatMessage message =
            JsonUtility.FromJson<ChatMessage>(json);

        if (message == null)
        {
            Debug.LogError("Failed to parse chat message!");
            return;
        }

        chatMessagesText.text +=
            message.name + ": " + message.message + "\n";

        if (playerManager.TryGetPlayerObject(
            message.id,
            out GameObject playerObject))
        {
            ShowChatBubble(playerObject, message.message);
        }

        Debug.Log(
            "Chat message received: " +
            message.name + ": " + message.message
        );
    }

    private void OnChatMessageSubmitted(string value)
    {
        SendChatMessage();
    }

    private void ShowChatBubble(
    GameObject playerObject,
    string message)
    {
        GameObject bubble = Instantiate(
            chatBubblePrefab,
            chatCanvas.transform
        );

        TextMeshProUGUI messageText =
            bubble.GetComponentInChildren<TextMeshProUGUI>();

        if (messageText != null)
        {
            messageText.text = message;
        }

        ChatBubbleFollower follower =
            bubble.GetComponent<ChatBubbleFollower>();

        if (follower != null)
        {
            follower.SetTarget(playerObject.transform);
        }
    }
}