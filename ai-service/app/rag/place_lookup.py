from app.db.postgres import connect


def resolve_place_id(place_slug: str) -> int | None:
    query = """
        SELECT id
        FROM places
        WHERE slug = %s
          AND active = TRUE
    """

    with connect() as connection:
        with connection.cursor() as cursor:
            cursor.execute(query, (place_slug,))
            row = cursor.fetchone()

    if row is None:
        return None

    return row[0]