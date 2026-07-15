import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronDown, Search } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { REGIONS, SIDO_LIST } from "@/data/regions";
import { cn } from "@/lib/utils";

// 특별시/광역시/특별자치시 여부 — 이 경우 "시/군" 단계 없이 바로 구를 고른다.
const METRO_RE = /(특별시|광역시|특별자치시)$/;
const isMetroCity = (name: string) => METRO_RE.test(name);

// 조건부로 나타나는 Select를 감싸 등장 애니메이션을 준다.
const revealClass = "animate-in fade-in-0 slide-in-from-top-1 duration-200";

// 드롭다운(Viewport)을 약 5개 높이로 제한하고 초과분은 스크롤 (항목 1개 ≈ 32px).
// popper 모드 기본 Viewport의 고정 높이(h-[trigger-height])는 h-auto로 무력화하고,
// max-height는 important(!)로 radix 인라인 스타일보다 우선 적용.
const dropdownViewportClass = "h-auto max-h-[168px]!";

// 히어로 검색창 — 클릭 시 하얀 패널이 아래로 펼쳐지고,
// 도/광역시 선택에 따라 하위 Select(시/군, 구)가 순차적으로 나타난다.
function HeroSearch() {
  const navigate = useNavigate();
  const rootRef = useRef<HTMLDivElement>(null);

  const [open, setOpen] = useState(false);
  const [sido, setSido] = useState("");
  const [sigungu, setSigungu] = useState("");
  const [gu, setGu] = useState("");
  const [keyword, setKeyword] = useState("");
  const [alertOpen, setAlertOpen] = useState(false);
  const [alertMsg, setAlertMsg] = useState("");

  // 검색 영역 밖을 클릭하면 패널을 닫는다 (Select 드롭다운·모달 등 radix 포털은 예외).
  useEffect(() => {
    if (!open) {
      return;
    }
    const handlePointerDown = (e: PointerEvent) => {
      const target = e.target as Node | null;
      if (!target) {
        return;
      }
      if (rootRef.current?.contains(target)) {
        return;
      }
      const el = target instanceof Element ? target : target.parentElement;
      if (
        el?.closest(
          "[data-radix-popper-content-wrapper],[data-slot='select-content'],[data-slot='dialog-content'],[data-slot='dialog-overlay']",
        )
      ) {
        return;
      }
      setOpen(false);
    };
    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [open]);

  const isMetro = isMetroCity(sido);
  const sigunguOptions = sido ? Object.keys(REGIONS[sido]) : [];
  const guOptions = sido && sigungu ? (REGIONS[sido][sigungu] ?? []) : [];

  // 도: 시/군 선택 후 구가 있을 때 노출 / 광역시: 선택 즉시 구 노출(sigungu 자동 설정)
  const showSigungu = sido !== "" && !isMetro;
  const showGu = sido !== "" && guOptions.length > 0;

  const handleSido = (value: string) => {
    setSido(value);
    setGu("");
    // 광역시는 시/군이 자기 자신 → 자동 설정하고 단계 스킵
    setSigungu(isMetroCity(value) ? value : "");
  };
  const handleSigungu = (value: string) => {
    setSigungu(value);
    setGu("");
  };

  const handleSearch = () => {
    if (!sido) {
      setAlertMsg("도/광역시를 선택해 주세요.");
      setAlertOpen(true);
      return;
    }
    // 특별시/광역시는 구가 필수 (세종처럼 하위 구가 없으면 제외)
    if (isMetro && guOptions.length > 0 && !gu) {
      setAlertMsg("구를 선택해 주세요.");
      setAlertOpen(true);
      return;
    }
    // 도는 시/군까지 필수
    if (!isMetro && !sigungu) {
      setAlertMsg("시/군을 선택해 주세요.");
      setAlertOpen(true);
      return;
    }
    navigate("/properties");
  };

  return (
    <div ref={rootRef} className="relative">
      <div className="rounded-[1.5rem] border border-white/15 bg-white/10 p-2 backdrop-blur-xl">
        <div className="overflow-hidden rounded-[1.1rem] bg-white text-slate-900">
          {/* 검색 트리거 행 */}
          <div className="flex items-center gap-3 p-3">
            <Search className="ml-1 size-5 shrink-0 text-slate-400" />
            <input
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onFocus={() => setOpen(true)}
              placeholder="원하는 지역이나 역 이름 검색"
              className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-slate-500"
            />
            <button
              type="button"
              aria-label={open ? "검색 옵션 접기" : "검색 옵션 펼치기"}
              onClick={() => setOpen((v) => !v)}
              className="shrink-0 rounded-full p-1 text-slate-400 transition-colors hover:text-slate-600"
            >
              <ChevronDown
                className={cn(
                  "size-4 transition-transform",
                  open && "rotate-180",
                )}
              />
            </button>
          </div>

          {/* 펼쳐지는 하얀 패널 */}
          <div
            className={cn(
              "grid transition-[grid-template-rows] duration-300 ease-out",
              open ? "grid-rows-[1fr]" : "grid-rows-[0fr]",
            )}
          >
            <div className="overflow-hidden">
              <div className="space-y-2 border-t border-slate-100 p-4">
                {/* 1단계: 도 / 광역시 (항상 노출) */}
                <Select value={sido || undefined} onValueChange={handleSido}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="도 / 광역시" />
                  </SelectTrigger>
                  <SelectContent
                    position="popper"
                    viewportClassName={dropdownViewportClass}
                  >
                    {SIDO_LIST.map((s) => (
                      <SelectItem key={s} value={s}>
                        {s}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                {/* 2단계: 시/군 — 도를 선택했을 때만 등장 (광역시는 스킵) */}
                {showSigungu && (
                  <div className={revealClass}>
                    <Select
                      value={sigungu || undefined}
                      onValueChange={handleSigungu}
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="시 / 군" />
                      </SelectTrigger>
                      <SelectContent
                        position="popper"
                        viewportClassName={dropdownViewportClass}
                      >
                        {sigunguOptions.map((s) => (
                          <SelectItem key={s} value={s}>
                            {s}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                )}

                {/* 3단계: 구 — 광역시는 즉시(필수), 도는 시/군 선택 후(선택) 등장 */}
                {showGu && (
                  <div className={revealClass}>
                    <Select value={gu || undefined} onValueChange={setGu}>
                      <SelectTrigger className="w-full">
                        <SelectValue
                          placeholder={isMetro ? "구 (필수)" : "구 (선택)"}
                        />
                      </SelectTrigger>
                      <SelectContent
                        position="popper"
                        viewportClassName={dropdownViewportClass}
                      >
                        {guOptions.map((g) => (
                          <SelectItem key={g} value={g}>
                            {g}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                )}

                <Button className="w-full rounded-xl" onClick={handleSearch}>
                  <Search /> 검색
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 필수 입력 안내 모달 */}
      <Dialog open={alertOpen} onOpenChange={setAlertOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>지역을 선택해 주세요</DialogTitle>
            <DialogDescription>{alertMsg}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button onClick={() => setAlertOpen(false)}>확인</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default HeroSearch;
