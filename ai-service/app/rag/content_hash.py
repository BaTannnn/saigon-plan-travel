import hashlib
def compute_content_hash(content: str) -> str:
    normalized_content = content.strip()

    return hashlib.sha256(
        normalized_content.encode("utf-8")
    ).hexdigest()