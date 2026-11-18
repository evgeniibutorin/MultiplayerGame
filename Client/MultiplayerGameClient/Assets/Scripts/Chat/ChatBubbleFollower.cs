
using UnityEngine;

public class ChatBubbleFollower : MonoBehaviour
{
    private Transform target;

    private Vector3 offset = new Vector3(0, 1.8f, 0);

    private const float LifetimeSeconds = 3f;

    public void SetTarget(Transform playerTransform)
    {
        target = playerTransform;

        Destroy(gameObject, LifetimeSeconds);
    }

    private void LateUpdate()
    {
        if (target == null)
        {
            Destroy(gameObject);
            return;
        }

        Vector3 worldPosition = target.position + offset;

        Vector3 screenPosition =
            Camera.main.WorldToScreenPoint(worldPosition);

        if (screenPosition.z <= 0)
        {
            gameObject.SetActive(false);
            return;
        }

        gameObject.SetActive(true);

        transform.position = screenPosition;
    }
}
