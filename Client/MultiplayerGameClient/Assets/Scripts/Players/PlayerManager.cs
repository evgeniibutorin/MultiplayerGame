
using System.Collections.Generic;
using TMPro;
using UnityEngine;

public class PlayerManager : MonoBehaviour
{
    [Header("Player Prefabs")]

    [SerializeField]
    private GameObject playerPrefab;

    [SerializeField]
    private GameObject playerNamePrefab;

    private Dictionary<string, GameObject> playerObjects =
        new Dictionary<string, GameObject>();

    public GameObject CreatePlayer(PlayerData player)
    {
        if (playerObjects.ContainsKey(player.id))
        {
            return playerObjects[player.id];
        }

        GameObject playerObject = Instantiate(
            playerPrefab,
            new Vector3(player.x, player.y, 0),
            Quaternion.identity
        );

        SpriteRenderer spriteRenderer =
            playerObject.GetComponent<SpriteRenderer>();

        if (spriteRenderer != null &&
            ColorUtility.TryParseHtmlString(
                player.color,
                out Color playerColor))
        {
            spriteRenderer.color = playerColor;
        }

        GameObject nameObject = Instantiate(
            playerNamePrefab,
            playerObject.transform
        );

        nameObject.transform.localPosition =
            new Vector3(0, -1.6f, 0);

        TextMeshPro text =
            nameObject.GetComponent<TextMeshPro>();

        if (text != null)
        {
            text.text = player.name;
        }

        playerObjects.Add(player.id, playerObject);

        Debug.Log("Player created: " + player.name);

        return playerObject;
    }

    public void MovePlayer(string playerId, float x, float y)
    {
        if (playerObjects.TryGetValue(
            playerId,
            out GameObject playerObject))
        {
            playerObject.transform.position =
                new Vector3(x, y, 0);
        }
    }
    public void RemovePlayer(string playerId)
    {
        if (playerObjects.TryGetValue(
            playerId,
            out GameObject playerObject))
        {
            Destroy(playerObject);

            playerObjects.Remove(playerId);

            Debug.Log("Player removed: " + playerId);
        }
    }

    public bool TryGetPlayerObject(
        string playerId,
        out GameObject playerObject)
    {
        return playerObjects.TryGetValue(
            playerId,
            out playerObject
        );
    }
}
