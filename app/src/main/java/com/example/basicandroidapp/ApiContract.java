package com.example.basicandroidapp;

import java.util.List;
import java.util.Map;

/**
 * Shared API field names and DTOs for the checklist review flow.
 */
final class ApiContract {
    private ApiContract() {
    }

    // 에뮬레이터: 10.0.2.2 / 실기기: Mac LAN IP (예: 192.168.x.x)
    @SuppressWarnings("java:S1313")
    static final String BASE_URL = "http://10.0.2.2:8000";

    static final String EXTRA_CONTRACT_TEXT = "contractText";
    static final String EXTRA_SESSION_ID = "sessionId";
    static final String EXTRA_TEMPLATE_ID = "templateId";

    static final class Endpoints {
        static final String REVIEW_SESSIONS = "/api/review-sessions";
        static final String SESSION_ANSWERS = "/api/review-sessions/{sessionId}/answers";
        static final String IMPORT_AI_FINDINGS = "/findings/import-ai";
    }

    static final class Keys {
        static final String DATA = "data";
        static final String ERROR = "error";
        static final String MESSAGE = "message";
        static final String STATUS_CODE = "statusCode";

        static final String SESSION = "session";
        static final String TEMPLATE = "template";
        static final String SUMMARY = "summary";
        static final String METADATA = "metadata";

        static final String ID = "id";
        static final String TEMPLATE_ID = "templateId";
        static final String CONTRACT_TYPE = "contractType";
        static final String USER_ROLE = "userRole";
        static final String STATUS = "status";
        static final String PROPERTY_ADDRESS = "propertyAddress";
        static final String CONTRACT_START_DATE = "contractStartDate";

        static final String SECTIONS = "sections";
        static final String TITLE = "title";
        static final String ITEMS = "items";
        static final String QUESTION = "question";
        static final String ANSWER_TYPE = "answerType";
        static final String RISK_WEIGHT = "riskWeight";
        static final String TOXIC_CLAUSE_TAGS = "toxicClauseTags";

        static final String ANSWERS = "answers";
        static final String ITEM_ID = "itemId";
        static final String VALUE = "value";
        static final String NOTE = "note";
        static final String SOURCE = "source";

        static final String FINDINGS = "findings";
        static final String RISK_LEVEL = "riskLevel";
        static final String EVIDENCE = "evidence";
        static final String TAG_IDS = "tagIds";
        static final String LEGAL_BASIS_IDS = "legalBasisIds";
        static final String PAIR_TYPE_ID = "pairTypeId";

        static final String TOTAL_ITEMS = "totalItems";
        static final String COMPLETED_ITEMS = "completedItems";
        static final String RISKY_ITEMS = "riskyItems";
        static final String COVERAGE_RATE = "coverageRate";
        static final String RISK_SCORE = "riskScore";
        static final String AI_FINDING_COUNT = "aiFindingCount";

        static final String TOXIC_CLAUSE_TAG_DICTIONARY = "toxicClauseTags";
        static final String LABEL = "label";
        static final String RISK_LABELS = "riskLabels";
        static final String PAIR_TYPES = "pairTypes";
        static final String DATASETS = "datasets";
        static final String LAW_AND_PRECEDENT = "lawAndPrecedent";
        static final String INPUT_CONTRACTS = "inputContracts";
        static final String RISK_SIGNAL_APIS = "riskSignalApis";
    }

    static final class Values {
        static final String CONTRACT_TYPE_JEONSE = "jeonse";
        static final String CONTRACT_TYPE_MONTHLY_RENT = "monthly-rent";

        static final String USER_ROLE_TENANT = "tenant";
        static final String USER_ROLE_LANDLORD = "landlord";
        static final String USER_ROLE_AGENT = "agent";
        static final String USER_ROLE_ADMIN = "admin";

        static final String SESSION_STATUS_DRAFT = "draft";

        static final String ANSWER_TYPE_BOOLEAN = "boolean";

        static final String RISK_LEVEL_DANGER = "danger";
        static final String RISK_LEVEL_CAUTION = "caution";
        static final String RISK_LEVEL_SAFE = "safe";

        static final String SOURCE_MANUAL = "manual";
        static final String SOURCE_AI = "ai";
    }

    static final class ApiResponse<T> {
        T data;
        ApiError error;
    }

    static final class ApiError {
        String message;
        int statusCode;
    }

    static final class ReviewSessionData {
        ReviewSession session;
        ChecklistTemplate template;
    }

    static final class ReviewSummaryData {
        ReviewSession session;
        ReviewSummary summary;
    }

    static final class SaveAnswersRequest {
        List<Answer> answers;
    }

    static final class ImportFindingsRequest {
        List<Finding> findings;
    }

    static final class ReviewSession {
        String id;
        String templateId;
        String contractType;
        String userRole;
        String status;
        SessionMetadata metadata;
    }

    static final class SessionMetadata {
        String propertyAddress;
        String contractStartDate;
    }

    static final class ChecklistTemplate {
        String id;
        List<TemplateSection> sections;
    }

    static final class TemplateSection {
        String title;
        List<ChecklistItem> items;
    }

    static final class ChecklistItem {
        String id;
        String question;
        String answerType;
        Integer riskWeight;
        List<String> toxicClauseTags;
    }

    static final class Answer {
        String itemId;
        boolean value;
        String note;
        String source = Values.SOURCE_MANUAL;
    }

    static final class Finding {
        String itemId;
        String riskLevel;
        String summary;
        List<String> evidence;
        String source = Values.SOURCE_AI;
        List<String> tagIds;
        List<String> legalBasisIds;
        String pairTypeId;
    }

    static final class ReviewSummary {
        int totalItems;
        int completedItems;
        int riskyItems;
        float coverageRate;
        int riskScore;
        String riskLevel;
        int aiFindingCount;
    }

    static final class StaticReferenceData {
        List<ToxicClauseTag> toxicClauseTags;
        List<RiskLabel> riskLabels;
        List<PairType> pairTypes;
        Datasets datasets;
    }

    static final class ToxicClauseTag {
        String id;
        String label;
    }

    static final class RiskLabel {
        String id;
        String label;
    }

    static final class PairType {
        String id;
        String label;
    }

    static final class Datasets {
        List<Map<String, Object>> lawAndPrecedent;
        List<Map<String, Object>> inputContracts;
        List<Map<String, Object>> riskSignalApis;
    }
}
