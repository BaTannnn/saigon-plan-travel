from app.db.postgres import get_connection


def resolve_place_id(place_slug: str) -> int | None:
    query = """
        SELECT id
        FROM places
        WHERE slug = %s
          AND active = TRUE
    """

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                query,
                (place_slug,),
            )

            row = cursor.fetchone()

    if row is None:
        return None

    return row[0]
