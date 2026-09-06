import os

from dotenv import load_dotenv
from pgvector.psycopg import register_vector
from psycopg import Connection
from psycopg_pool import ConnectionPool

load_dotenv()


def get_database_url() -> str:
    database_url = os.getenv("DATABASE_URL")

    if not database_url:
        raise RuntimeError("DATABASE_URL environment variable is required")

    return database_url


def configure_connection(connection: Connection) -> None:
    register_vector(connection)


pool = ConnectionPool(
    conninfo=get_database_url(),
    min_size=1,
    max_size=5,
    open=False,
    configure=configure_connection,
)


def get_connection():
    return pool.connection()
