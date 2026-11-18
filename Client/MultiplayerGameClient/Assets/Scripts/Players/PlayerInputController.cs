
using UnityEngine;
using UnityEngine.InputSystem;

public class PlayerInputController : MonoBehaviour
{
    [Header("Network Manager")]

    [SerializeField]
    private GameNetworkManager networkManager;

    private void Update()
    {
        if (Keyboard.current == null)
        {
            return;
        }

        if (Keyboard.current.upArrowKey.wasPressedThisFrame)
        {
            networkManager.SendMoveMessageToServer("UP");
        }

        if (Keyboard.current.downArrowKey.wasPressedThisFrame)
        {
            networkManager.SendMoveMessageToServer("DOWN");
        }

        if (Keyboard.current.leftArrowKey.wasPressedThisFrame)
        {
            networkManager.SendMoveMessageToServer("LEFT");
        }

        if (Keyboard.current.rightArrowKey.wasPressedThisFrame)
        {
            networkManager.SendMoveMessageToServer("RIGHT");
        }
    }
}
