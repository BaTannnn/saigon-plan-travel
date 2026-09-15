from fastapi import APIRouter

from app.assistant.assistant_service import answer_assistant_message
from app.assistant.models import AssistantRequest, AssistantResponse

router = APIRouter(
    prefix="/api/v1/assistant",
    tags=["Assistant"],
)


@router.post("/messages", response_model=AssistantResponse)
def create_assistant_message(request: AssistantRequest) -> AssistantResponse:
    return answer_assistant_message(request)
