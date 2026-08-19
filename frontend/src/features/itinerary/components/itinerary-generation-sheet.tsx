"use client";

import { FormEvent, useState } from "react";
import { Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { ItineraryGenerationPreview } from "@/features/itinerary/components/itinerary-generation-preview";
import { ApiError } from "@/lib/api/api-client";
import type { ItineraryGenerationPreviewResponse } from "@/types/itinerary";

type ItineraryGenerationSheetProps = {
  onGenerate: (
    preferenceDescription: string,
  ) => Promise<ItineraryGenerationPreviewResponse>;
  onApply: (placeSlugs: string[]) => Promise<void>;
};

function getGenerationErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 0) {
      return "Không thể kết nối đến máy chủ.";
    }

    return error.problem?.detail ?? "Không thể tạo gợi ý hành trình.";
  }

  return "Đã xảy ra lỗi khi tạo gợi ý hành trình.";
}

export function ItineraryGenerationSheet({
  onGenerate,
  onApply,
}: ItineraryGenerationSheetProps) {
  const [open, setOpen] = useState(false);
  const [preferenceDescription, setPreferenceDescription] = useState("");
  const [generationPreview, setGenerationPreview] =
    useState<ItineraryGenerationPreviewResponse | null>(null);
  const [generationLoading, setGenerationLoading] = useState(false);
  const [generationError, setGenerationError] = useState<string | null>(null);
  const [applyLoading, setApplyLoading] = useState(false);
  const [applyError, setApplyError] = useState<string | null>(null);

  const preference = preferenceDescription.trim();

  async function handleGenerate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!preference || generationLoading || applyLoading) return;

    setGenerationLoading(true);
    setGenerationError(null);
    setApplyError(null);
    setGenerationPreview(null);

    try {
      const response = await onGenerate(preference);
      setGenerationPreview(response);
    } catch (error) {
      setGenerationError(getGenerationErrorMessage(error));
    } finally {
      setGenerationLoading(false);
    }
  }

  async function handleApply() {
    if (
      !generationPreview ||
      generationPreview.stops.length === 0 ||
      applyLoading
    ) {
      return;
    }

    const placeSlugs = generationPreview.stops.map(
      (stop) => stop.place.slug,
    );

    setApplyLoading(true);
    setApplyError(null);

    try {
      await onApply(placeSlugs);
      setGenerationPreview(null);
      setGenerationError(null);
      setApplyError(null);
      setPreferenceDescription("");
      setOpen(false);
    } catch {
      setApplyError("Không thể áp dụng hành trình. Vui lòng thử lại.");
    } finally {
      setApplyLoading(false);
    }
  }

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen && applyLoading) return;
    setOpen(nextOpen);
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button type="button" variant="outline" size="sm">
          <Sparkles className="size-4 text-primary" aria-hidden="true" />
          Tạo hành trình tự động
        </Button>
      </DialogTrigger>

      <DialogContent
        className={
          generationPreview
            ? "w-[min(calc(100vw_-_2rem),680px)]"
            : "w-[min(calc(100vw_-_2rem),460px)]"
        }
      >
        <DialogHeader className="border-b border-border pb-5">
          <DialogTitle>
            Tạo hành trình tự động
          </DialogTitle>
          <DialogDescription>
            {generationPreview
              ? "Đây là bản đề xuất, chưa thay đổi hành trình hiện tại."
              : "Mô tả kiểu chuyến đi bạn muốn."}
          </DialogDescription>
        </DialogHeader>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-6">
          <form className="grid gap-3 pt-5" onSubmit={handleGenerate}>
            <div className="grid gap-1.5">
              <Label
                className="text-xs font-extrabold tracking-[0.1em] text-text-secondary uppercase"
                htmlFor="itinerary-generation-preference"
              >
                Chuyến đi mong muốn
              </Label>
              <textarea
                id="itinerary-generation-preference"
                value={preferenceDescription}
                onChange={(event) =>
                  setPreferenceDescription(event.target.value)
                }
                placeholder="Ví dụ: Tôi muốn tham quan nhẹ nhàng, thích lịch sử, kiến trúc và quán cà phê..."
                rows={generationPreview ? 4 : 5}
                disabled={generationLoading || applyLoading}
                className="min-h-28 w-full resize-y rounded-lg border border-input bg-surface px-3 py-2 text-sm text-text-primary outline-none transition placeholder:text-text-secondary/70 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/25 disabled:cursor-not-allowed disabled:opacity-50"
              />
            </div>

            <div className="flex flex-wrap items-center justify-end gap-2">
              <Button
                type="submit"
                variant={generationPreview ? "outline" : "accent"}
                disabled={!preference || generationLoading || applyLoading}
              >
                <Sparkles className="size-4" aria-hidden="true" />
                {generationLoading
                  ? "Đang tạo hành trình..."
                  : generationPreview
                    ? "Tạo lại"
                    : "Tạo gợi ý"}
              </Button>
              {!generationPreview ? (
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setOpen(false)}
                  disabled={applyLoading}
                >
                  Hủy
                </Button>
              ) : null}
            </div>
          </form>

          {generationError ? (
            <p
              className="mt-4 rounded-lg bg-destructive/10 px-3 py-2 text-sm font-semibold text-destructive"
              role="alert"
            >
              {generationError}
            </p>
          ) : null}

          {generationPreview ? (
            <ItineraryGenerationPreview
              preview={generationPreview}
              applyError={applyError}
              applyLoading={applyLoading}
              generationLoading={generationLoading}
              onApply={handleApply}
            />
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
  );
}
