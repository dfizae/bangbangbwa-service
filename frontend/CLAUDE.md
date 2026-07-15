@AGENTS.md

# 프로젝트: 방방봐(방을 방송으로 봐)(서비스명) 메인페이지

## 필수 참조

- 모든 UI 작업 전에 docs/frontend-spec.md의 기능명세서를 먼저 읽고 따를 것
- 디자인 토큰: 블루 계열 신뢰 톤, Tailwind 테마 변수 사용
- 스택: React + Vite + TypeScript + Tailwind (패키지 매니저: pnpm)

## 작업 규칙

- 섹션 단위로 구현하고, 각 섹션 완료 후 확인 요청
- 명세서에 없는 기능은 임의로 추가하지 말 것

## 디자인 규칙

- UI 컴포넌트는 shadcn/ui를 우선 사용 (Button, Card, Badge, Input, Select, Dialog, Tabs)
- shadcn/ui에 없는 것만 직접 구현하고, 스타일은 Tailwind 유틸리티만 사용
- 컬러: primary는 블루 계열(신뢰 톤), 텍스트·서피스는 slate 계열. CSS 변수(테마)로 정의
  - 라이트(배경 화이트) 전용 — 다크 모드는 사용하지 않음(팀 협의로 제거). 테마 토글·`.dark` 블록 없음
  - 색상 추가·변경 시 index.css의 `:root` 블록에 정의할 것
- 폰트: IBM Plex Sans KR (index.html Google Fonts 로드, `--font-sans`) — design.pen과 동일. 한글 블록에는 word-break: keep-all 유지
- 타이포: 제목 font-semibold, 본문 font-normal, 크기 단계는 text-sm/base/lg/xl/2xl만 사용 (랜딩 히어로·섹션 제목은 예외)
- 간격은 4px 그리드(p-2/4/6/8), 카드 radius는 rounded-xl, 그림자는 shadow-sm까지만
  - 예외: 랜딩의 히어로 데모 카드·리포트 목업은 블루 틴티드 섀도우(rgba(22,93,252,…)) 사용 — 검정 그림자 금지
- 금지: 그라데이션 남발, 3색 이상 포인트 컬러, 이모지 아이콘(lucide-react 아이콘 사용)
- 레이아웃: 랜딩 섹션은 인접 섹션과 다른 패턴 사용(3등분 균일 카드 금지 — Bento·타임라인 등). 상세 원칙은 supanova-design-skill/redesign-skill/SKILL.md 참조
- UI 작업 시작 전 frontend-design 스킬(설치돼 있다면)을 로드해 미적 방향을 적용할 것
