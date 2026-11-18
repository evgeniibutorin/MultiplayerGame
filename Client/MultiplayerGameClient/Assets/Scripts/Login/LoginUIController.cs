
using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class LoginUIController : MonoBehaviour
{
    [Header("Login UI")]

    [SerializeField]
    private GameObject loginPanel;

    [SerializeField]
    private GameObject gamePanel;

    [SerializeField]
    private TMP_InputField playerNameInputField;

    [SerializeField]
    private Button connectButton;

    [Header("Network Manager")]

    [SerializeField]
    private GameNetworkManager networkManager;

    private void Start()
    {
        playerNameInputField.characterLimit = 15;
        connectButton.onClick.AddListener(ConnectToServer);
        playerNameInputField.onSubmit.AddListener(OnPlayerNameSubmitted);
    }

    private void ConnectToServer()
    {
        string playerName = playerNameInputField.text.Trim();

        if (playerName.Length < 2 || playerName.Length > 15)
        {
            Debug.LogWarning("Имя должно содержать от 2 до 15 символов!");
            return;
        }

        networkManager.ConnectToServer(playerName);
    }

    private void OnPlayerNameSubmitted(string value)
    {
        ConnectToServer();
    }

    public void ShowGameScreen()
    {
        loginPanel.SetActive(false);
        gamePanel.SetActive(true);
    }
}
