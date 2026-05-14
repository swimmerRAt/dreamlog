# 프론트엔드·풀스택 연동 변수 정리

전월세 계약서 체크리스트 백엔드(Java) 기준으로, 프론트엔드와 풀스택에서 자주 쓰게 될 변수명과 역할을 빠르게 확인할 수 있도록 정리한 문서입니다.

| 항목 | 내용 |
| --- | --- |
| 문서 목적 | 연동 시 필요한 주요 변수, 타입 의미, 사용 위치를 팀 공용 기준으로 통일 |
| 대상 팀 | 프론트엔드 / 풀스택 / AI 결과 연동 담당 |

## 1. 공통 응답 규칙

| 변수명 | 타입 | 기능/의미 | 사용 위치 또는 비고 |
| --- | --- | --- | --- |
| `data` | `object` | 모든 성공 응답의 루트 payload | 프론트는 항상 `data`부터 파싱. 예: `data.session.id`, `data.summary.riskLevel` |
| `error.message` | `string` | 실패 시 오류 메시지 | 사용자 알림, 디버깅 메시지 표시 |
| `error.statusCode` | `number` | 실패 상태 코드 | `400`, `401`, `404` 등 화면 분기 처리 |

## 2. 세션 관련 변수

| 변수명 | 타입 | 기능/의미 | 사용 위치 또는 비고 |
| --- | --- | --- | --- |
| `data.session.id` | `string` | 리뷰 세션 고유 식별자 | 답변 저장, 요약 조회, AI finding 저장에 계속 사용 |
| `data.session.templateId` | `string` | 체크리스트 템플릿 식별자 | 템플릿 버전 동기화 확인 |
| `data.session.contractType` | `enum` | 계약 유형 | `jeonse`, `monthly-rent` 화면 분기 |
| `data.session.userRole` | `enum` | 사용자 역할 | `tenant`, `landlord`, `agent`, `admin` 권한 또는 화면 분기 |
| `data.session.status` | `string` | 세션 진행 상태 | 현재는 `draft`, 이후 완료 상태로 확장 가능 |
| `data.session.metadata.propertyAddress` | `string \| null` | 계약 대상 주소 | 지역 위험도 조회나 주소 표시에 사용 |
| `data.session.metadata.contractStartDate` | `string \| null` | 계약 시작일 | 기간 계산, 갱신 관련 로직 확장 |

## 3. 템플릿 및 체크리스트 렌더링 변수

| 변수명 | 타입 | 기능/의미 | 사용 위치 또는 비고 |
| --- | --- | --- | --- |
| `data.template.id` | `string` | 체크리스트 템플릿 식별자 | 프론트 템플릿 로딩 기준 |
| `data.template.sections` | `array` | 섹션 목록 | 화면에서 단계·주제별 묶음으로 렌더링 |
| `data.template.sections[].title` | `string` | 섹션 제목 | 사용자에게 보이는 분류명 |
| `data.template.sections[].items` | `array` | 질문 목록 | 실제 체크리스트 반복 렌더링 |
| `data.template.sections[].items[].id` | `string` | 질문 식별자 | `answers[].itemId`, `findings[].itemId`와 연결 |
| `data.template.sections[].items[].question` | `string` | 질문 문구 | 체크리스트 본문 표시 |
| `data.template.sections[].items[].answerType` | `string` | 입력 형식 힌트 | `boolean` 체크 여부 등 UI 구성 참고 |
| `data.template.sections[].items[].riskWeight` | `number \| null` | 위험 점수 가중치 | `summary.riskScore` 계산 기준 |
| `data.template.sections[].items[].toxicClauseTags` | `array` | 연결된 독소조항 태그 | 질문과 태그 연결 표시 |

## 4. 답변 저장 변수

| 변수명 | 타입 | 기능/의미 | 사용 위치 또는 비고 |
| --- | --- | --- | --- |
| `answers` | `array` | 답변 저장 요청의 루트 배열 | `PATCH /api/review-sessions/{sessionId}/answers` 요청 body |
| `answers[].itemId` | `string` | 어느 질문에 대한 답인지 연결 | 템플릿 item id와 반드시 일치 |
| `answers[].value` | `boolean` | 질문 충족 여부 | `false`이면 위험 점수 계산에 반영될 수 있음 |
| `answers[].note` | `string \| null` | 사용자 메모 | 보충 설명, 주석, 내부 검토용 |
| `answers[].source` | `string` | 입력 출처 | 기본값은 `manual`, 추후 OCR/AI/manual 구분 가능 |

## 5. AI finding 및 풀스택 분석 변수

| 변수명 | 타입 | 기능/의미 | 사용 위치 또는 비고 |
| --- | --- | --- | --- |
| `findings` | `array` | AI 결과 저장 요청의 루트 배열 | `POST /findings/import-ai` body |
| `findings[].itemId` | `string` | 어느 체크 항목에 대한 finding인지 연결 | 템플릿 item id와 일치해야 함 |
| `findings[].riskLevel` | `enum` | 개별 finding 위험도 | `danger`, `caution`, `safe`. UI 배지 색상과 직접 연결 |
| `findings[].summary` | `string` | 짧은 요약 설명 | 사용자 결과 카드에 노출 |
| `findings[].evidence` | `array<string>` | 근거 문자열 목록 | 조항 원문 일부, 추출 위치, 분석 근거 표시에 사용 |
| `findings[].source` | `string` | 분석 출처 | 기본값은 `ai`, 나중에 모델명/파이프라인 구분 가능 |
| `findings[].tagIds` | `array<string>` | 독소조항 유형 태그 id 목록 | 예: `implicit-renewal-refusal`, `mortgage-special-clause` |
| `findings[].legalBasisIds` | `array<string>` | 법령·판례 근거 id 목록 | 현재 `datasets.lawAndPrecedent` 범위만 허용 |
| `findings[].pairTypeId` | `string \| null` | 정상↔독소 대조쌍 기준 id | `law-vs-violation`, `hug-safe-vs-toxic` 등 기준 표시용 |

## 6. 요약 화면 변수

| 변수명 | 타입 | 기능/의미 | 사용 위치 또는 비고 |
| --- | --- | --- | --- |
| `data.summary.totalItems` | `number` | 전체 문항 수 | 진행률 계산 기준 |
| `data.summary.completedItems` | `number` | 답변 완료 문항 수 | 완료율 표시 |
| `data.summary.riskyItems` | `number` | 위험 응답으로 계산된 문항 수 | 경고 지표 표시 |
| `data.summary.coverageRate` | `number` | 진행률 | 프로그레스 바 표시에 사용 |
| `data.summary.riskScore` | `number` | 누적 위험 점수 | 점수 기반 요약 표시에 사용 |
| `data.summary.riskLevel` | `enum` | 세션 전체 위험도 | `danger`, `caution`, `safe`로 통일됨 |
| `data.summary.aiFindingCount` | `number` | AI가 생성한 finding 개수 | 분석 완료 여부 확인 |

## 7. 정적 기준 데이터 변수

| 변수명 | 타입 | 기능/의미 | 사용 위치 또는 비고 |
| --- | --- | --- | --- |
| `data.toxicClauseTags` | `array` | 독소조항 태그 사전 | 태그 id를 한글 라벨로 변환할 때 사용 |
| `data.toxicClauseTags[].id` | `string` | 태그 id | 백엔드 검증 기준값 |
| `data.toxicClauseTags[].label` | `string` | 태그 한글명 | 사용자 화면 표시용 |
| `data.riskLabels` | `array` | 위험도 라벨 사전 | `danger/caution/safe` 표시명 변환 |
| `data.pairTypes` | `array` | 대조쌍 기준 사전 | `pairTypeId` 설명 표시에 사용 |
| `data.datasets.lawAndPrecedent` | `array` | 법령·판례 데이터셋 목록 | `legalBasisIds` 설명 표시용 |
| `data.datasets.inputContracts` | `array` | 표준 계약서/입력 기준 데이터셋 | 학습·비교 기준 설명용 |
| `data.datasets.riskSignalApis` | `array` | 전세사기 위험지수용 외부 API 목록 | 후속 풀스택 연동 참고용 |

## 8. 프론트에서 먼저 고정하면 좋은 핵심 키

| 변수명 | 타입 | 기능/의미 | 사용 위치 또는 비고 |
| --- | --- | --- | --- |
| `data.session.id` | `string` | 세션 라우팅과 저장의 기준 키 | 가장 먼저 상태 관리 store에 보관 권장 |
| `data.template.sections[].items[].id` | `string` | 문항 기준 키 | UI item, answer, finding 연결의 중심 |
| `answers[].itemId` | `string` | 답변 저장 키 | 프론트 폼 state 저장 시 핵심 |
| `findings[].itemId` | `string` | 분석 결과 연결 키 | AI 결과 merge 시 핵심 |
| `findings[].riskLevel` | `enum` | finding 위험도 | 칩, 배지, 알림 스타일 기준 |
| `findings[].tagIds` | `array<string>` | 독소 태그 연결 | 세부 위험 종류 표시에 사용 |
| `findings[].legalBasisIds` | `array<string>` | 법적 근거 연결 | 근거 링크, 툴팁, 설명 패널 표시에 사용 |
| `data.summary.riskLevel` | `enum` | 세션 전체 위험도 | 최종 결과 헤더 표시에 사용 |

## 연동 주의 사항

- 성공 응답은 모두 `data`로 감싸져 있습니다.
- `riskLevel`은 `danger`, `caution`, `safe`만 사용합니다.
- `legalBasisIds`는 `lawAndPrecedent` 범위만 허용됩니다.
- 로컬 프론트 연결은 CORS origin pattern 기준으로 허용됩니다.
