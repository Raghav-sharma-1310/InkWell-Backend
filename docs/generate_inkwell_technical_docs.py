from __future__ import annotations

import re
import textwrap
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT_MD = ROOT / "docs" / "InkWell-Technical-Documentation.md"
OUT_PDF = ROOT / "docs" / "InkWell-Technical-Documentation.pdf"

SERVICES = [
    "discovery-service",
    "admin-server",
    "api-gateway",
    "auth-service",
    "user-service",
    "post-service",
    "comment-service",
    "category-service",
    "media-service",
    "newsletter-service",
    "notification-service",
    "payment-service",
    "subscription-service",
]

ACTUAL_SERVICES = {
    "discovery-service",
    "admin-server",
    "api-gateway",
    "auth-service",
    "post-service",
    "comment-service",
    "category-service",
    "media-service",
    "newsletter-service",
    "notification-service",
    "payment-service",
}

LOGICAL_SERVICE_NOTES = {
    "user-service": (
        "No standalone user-service module exists in this repository. User identity, "
        "profile, role, OAuth2, refresh-token, and internal user lookup behavior are "
        "implemented inside auth-service."
    ),
    "subscription-service": (
        "No standalone subscription-service module exists in this repository. "
        "Subscription tier/status and Razorpay-backed subscription payment state are "
        "implemented mainly inside auth-service, with payment-service acting as a "
        "smaller Razorpay utility boundary."
    ),
}

SERVICE_META = {
    "discovery-service": {
        "port": "8761",
        "summary": "Eureka registry used by every runtime service for service discovery.",
        "database": "None",
        "external": "Spring Cloud Netflix Eureka",
        "flow": "Services register with Eureka at startup. Gateway and Feign clients resolve logical service names through Eureka instead of hard-coded addresses.",
        "security": "Normally internal-only. It exposes actuator health for monitoring and does not enforce JWT for registry access.",
        "peers": ["api-gateway", "admin-server", "all services"],
    },
    "admin-server": {
        "port": "9090",
        "summary": "Spring Boot Admin dashboard for health, metrics, and operational monitoring.",
        "database": "None",
        "external": "Actuator endpoints from registered clients",
        "flow": "Each service registers its actuator URLs with Spring Boot Admin. Operators inspect health, metrics, and metadata through the admin UI.",
        "security": "The current code has a permissive SecurityConfig so local development monitoring is frictionless.",
        "peers": ["discovery-service", "all actuator-enabled services"],
    },
    "api-gateway": {
        "port": "8080",
        "summary": "Public backend entry point, route dispatcher, JWT validator, and gateway policy layer.",
        "database": "Redis for gateway rate limiting",
        "external": "Eureka, Redis, downstream services",
        "flow": "Incoming requests match Spring Cloud Gateway routes. JwtAuthenticationFilter validates protected requests, adds identity headers, and forwards to the discovered service instance.",
        "security": "Bearer JWT validation occurs at the edge. Public paths pass through; protected paths require a valid token and role/subscription constraints where configured.",
        "peers": ["auth-service", "post-service", "comment-service", "category-service", "media-service", "newsletter-service", "notification-service", "payment-service"],
    },
    "auth-service": {
        "port": "8081",
        "summary": "Identity, authentication, user profile, author request, feedback, OTP, email, and subscription payment state boundary.",
        "database": "MySQL auth_db plus Redis for rate limiting/temporary data",
        "external": "Email SMTP, OAuth2 providers, Razorpay API, Eureka, Admin Server",
        "flow": "Controllers validate DTOs, services apply business rules, repositories persist users/tokens/requests/payments, and mappers shape API responses.",
        "security": "Issues access and refresh JWTs. Gateway-authenticated internal calls pass identity headers that GatewayAuthenticationFilter converts into a principal.",
        "peers": ["api-gateway", "notification-service", "payment gateway", "mail server"],
    },
    "post-service": {
        "port": "8082",
        "summary": "Post authoring, publishing, public feed, likes, bookmarks, follows, and reading history.",
        "database": "MySQL post_db plus Redis cache",
        "external": "category-service through OpenFeign, RabbitMQ events, Eureka, Admin Server",
        "flow": "Controllers delegate to PostService and FollowBookmarkService. Repositories persist posts/social actions. CategoryClient syncs taxonomy. RabbitMQ publishes post events.",
        "security": "Gateway headers build GatewayUserPrincipal. Author/admin operations require author/admin role checks inside service logic.",
        "peers": ["category-service", "comment-service", "newsletter-service", "notification-service"],
    },
    "comment-service": {
        "port": "8084",
        "summary": "Threaded comments, replies, comment likes, and comment cleanup on deleted posts.",
        "database": "MySQL comment_db",
        "external": "post-service through OpenFeign, RabbitMQ events, Eureka, Admin Server",
        "flow": "CommentController validates comment DTOs, CommentService checks post metadata, sanitizes content, persists comments/likes, and publishes notification events.",
        "security": "GatewayAuthenticationFilter trusts identity headers from api-gateway and makes a GatewayUserPrincipal available to controllers/services.",
        "peers": ["post-service", "notification-service"],
    },
    "category-service": {
        "port": "8083",
        "summary": "Canonical category and tag taxonomy management.",
        "database": "MySQL category_db",
        "external": "RabbitMQ post.deleted events, Eureka, Admin Server",
        "flow": "Admin/public/internal controllers call CategoryService. Repositories persist categories/tags and post taxonomy mappings.",
        "security": "Gateway identity headers support protected admin/internal endpoints.",
        "peers": ["post-service"],
    },
    "media-service": {
        "port": "8085",
        "summary": "Media upload, metadata persistence, and local/S3 storage abstraction.",
        "database": "MySQL media_db plus local filesystem or S3 object storage",
        "external": "Local disk or AWS S3, Eureka, Admin Server",
        "flow": "MediaController receives multipart uploads, MediaService validates and stores files through StorageService, then MediaRepository stores metadata.",
        "security": "Gateway-authenticated uploads use GatewayUserPrincipal; public file URLs can be served for read access.",
        "peers": ["api-gateway", "post-service/frontend consumers"],
    },
    "newsletter-service": {
        "port": "8086",
        "summary": "Newsletter subscriptions, confirmations, campaign records, templates, and campaign delivery.",
        "database": "MySQL newsletter_db",
        "external": "SMTP mail server, RabbitMQ post.published events, Eureka, Admin Server",
        "flow": "NewsletterController handles subscribe/confirm/unsubscribe/campaign endpoints. NewsletterService persists subscribers/campaigns and MailService delivers messages.",
        "security": "Admin campaign endpoints are protected through gateway identity headers; public subscribe/confirm endpoints remain open.",
        "peers": ["post-service", "mail server"],
    },
    "notification-service": {
        "port": "8087",
        "summary": "In-app notifications, broadcasts, audit logs, and optional notification emails.",
        "database": "MySQL notification_db",
        "external": "auth-service through OpenFeign, RabbitMQ comment/reply/post events, SMTP, Eureka, Admin Server",
        "flow": "Controllers expose user notification and admin broadcast APIs. NotificationService persists notifications, handles Rabbit listeners, resolves users through AuthClient, and optionally sends email.",
        "security": "Gateway identity headers identify the current user. Admin broadcast endpoints require admin authority.",
        "peers": ["auth-service", "comment-service", "post-service", "mail server"],
    },
    "payment-service": {
        "port": "8088",
        "summary": "Small Razorpay adapter for order creation and signature verification.",
        "database": "None in current module",
        "external": "Razorpay SDK/API, Eureka, Admin Server",
        "flow": "PaymentController delegates to RazorpayService to create orders and verify signatures.",
        "security": "Routed through api-gateway; subscription state is stored in auth-service.",
        "peers": ["api-gateway", "auth-service logical subscription flow", "Razorpay"],
    },
}


@dataclass
class FileInfo:
    path: Path
    rel: str
    kind: str
    package: str = ""
    classes: list[str] = field(default_factory=list)
    annotations: list[str] = field(default_factory=list)
    endpoints: list[str] = field(default_factory=list)
    methods: list[str] = field(default_factory=list)


class DocBuilder:
    def __init__(self) -> None:
        self.md: list[str] = []
        self.blocks: list[dict[str, str]] = []

    def add(self, style: str, text: str = "") -> None:
        text = clean(text)
        self.blocks.append({"style": style, "text": text})
        if style == "title":
            self.md.append(f"# {text}\n")
        elif style == "h1":
            self.md.append(f"\n# {text}\n")
        elif style == "h2":
            self.md.append(f"\n## {text}\n")
        elif style == "h3":
            self.md.append(f"\n### {text}\n")
        elif style == "bullet":
            self.md.append(f"- {text}\n")
        elif style == "code":
            self.md.append(f"```\n{text}\n```\n")
        elif style == "mermaid":
            self.md.append(f"```mermaid\n{text}\n```\n")
        elif text:
            self.md.append(f"{text}\n")
        else:
            self.md.append("\n")


def clean(value: str) -> str:
    value = value.replace("\t", "    ")
    value = unicodedata.normalize("NFKD", value)
    value = value.encode("ascii", "ignore").decode("ascii")
    return value


def collect_files(service: str) -> list[FileInfo]:
    base = ROOT / service
    raw: list[Path] = []
    if (base / "pom.xml").exists():
        raw.append(base / "pom.xml")
    for folder in ["src/main/resources", "src/main/java", "src/test/resources", "src/test/java"]:
        start = base / folder
        if start.exists():
            raw.extend(p for p in start.rglob("*") if p.is_file())
    infos = [analyze_file(service, path) for path in raw]
    return sorted(infos, key=lambda item: sort_key(item.rel))


def sort_key(rel: str) -> tuple[int, str]:
    if rel == "pom.xml":
        return (0, rel)
    if rel.startswith("src/main/resources"):
        return (1, rel)
    if rel.startswith("src/main/java"):
        return (2, rel)
    if rel.startswith("src/test/resources"):
        return (3, rel)
    return (4, rel)


def analyze_file(service: str, path: Path) -> FileInfo:
    rel = path.relative_to(ROOT / service).as_posix()
    suffix = path.suffix.lower()
    text = ""
    if suffix in {".java", ".yml", ".yaml", ".xml", ".properties"}:
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            text = ""
    info = FileInfo(path=path, rel=rel, kind=classify(rel, text))
    if suffix == ".java":
        info.package = first_match(text, r"package\s+([\w.]+);")
        info.classes = re.findall(r"\b(?:class|interface|record|enum)\s+([A-Za-z0-9_]+)", text)
        info.annotations = unique(re.findall(r"@([A-Za-z0-9_]+)", text))[:12]
        info.endpoints = endpoint_lines(text)
        info.methods = method_names(text)[:18]
    return info


def classify(rel: str, text: str) -> str:
    name = Path(rel).name
    lower = rel.lower()
    if name == "pom.xml":
        return "Maven build descriptor"
    if name.startswith("application") and (name.endswith(".yml") or name.endswith(".yaml") or name.endswith(".properties")):
        return "Runtime configuration"
    if name == "package-info.java":
        return "Package documentation"
    if name.endswith("Application.java"):
        return "Spring Boot application bootstrap"
    if "/controller/" in lower:
        return "REST controller"
    if "/service/" in lower:
        return "Business service"
    if "/repository/" in lower:
        return "Spring Data repository"
    if "/entity/" in lower:
        return "JPA entity"
    if "/dto/" in lower:
        return "DTO/API contract"
    if "/security/" in lower:
        return "Security component"
    if "/config/" in lower:
        return "Spring configuration"
    if "/client/" in lower:
        return "Inter-service client"
    if "/exception/" in lower:
        return "Exception handling"
    if "/enumtype/" in lower:
        return "Domain enum"
    if "/util/" in lower:
        return "Utility helper"
    if "/storage/" in lower:
        return "Storage abstraction"
    if "/test/" in lower or name.endswith("Test.java") or name.endswith("Tests.java"):
        return "Automated test"
    if "@FeignClient" in text:
        return "Inter-service client"
    return "Source file"


def endpoint_lines(text: str) -> list[str]:
    endpoints = []
    for line in text.splitlines():
        stripped = line.strip()
        if re.match(r"@(Get|Post|Put|Patch|Delete|Request)Mapping", stripped):
            endpoints.append(stripped)
    return endpoints[:12]


def method_names(text: str) -> list[str]:
    matches = re.findall(
        r"\b(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\], ?]+\s+([a-zA-Z_][A-Za-z0-9_]*)\s*\(",
        text,
    )
    return unique([name for name in matches if name not in {"if", "for", "while", "switch"}])


def first_match(text: str, pattern: str) -> str:
    match = re.search(pattern, text)
    return match.group(1) if match else ""


def unique(items: list[str]) -> list[str]:
    result = []
    seen = set()
    for item in items:
        if item and item not in seen:
            result.append(item)
            seen.add(item)
    return result


def file_explanation(info: FileInfo) -> str:
    rel = info.rel
    kind = info.kind
    if kind == "Maven build descriptor":
        return "Defines the service artifact, inherited platform versions, runtime dependencies, and test dependencies. It is needed so Maven can build this service consistently as part of the multi-module platform."
    if kind == "Runtime configuration":
        return "Defines the service name, port, Eureka registration, datasource or external integrations, actuator exposure, and service-specific settings. Sensitive values should be supplied through environment variables rather than committed defaults."
    if kind == "Spring Boot application bootstrap":
        return "Starts the Spring Boot application context and enables the service's framework features. It is the executable entry point for local runs, containers, and tests."
    if kind == "REST controller":
        return "Exposes HTTP endpoints, accepts request DTOs and path/query parameters, delegates business rules to services, and returns a consistent ApiResponse shape."
    if kind == "Business service":
        return "Contains business logic, validation decisions, transaction boundaries, repository calls, messaging, and external-client orchestration. Controllers stay thin because this file owns behavior."
    if kind == "Spring Data repository":
        return "Provides database access through Spring Data JPA method names and generated implementations. It isolates persistence queries from service logic."
    if kind == "JPA entity":
        return "Models a database table or persisted aggregate. It is needed so JPA/Hibernate can map domain state to relational records."
    if kind == "DTO/API contract":
        return "Defines request or response data crossing service boundaries. DTOs keep API payloads independent from JPA entities and usually host validation annotations."
    if kind == "Security component":
        return "Participates in authentication or authorization, usually by configuring Spring Security, reading gateway identity headers, or representing the authenticated principal."
    if kind == "Spring configuration":
        return "Declares Spring beans for cross-cutting infrastructure such as OpenAPI, RabbitMQ, caching, storage, or seed data."
    if kind == "Inter-service client":
        return "Declares a typed client for calling another microservice by its Eureka service name. It avoids hard-coded URLs and keeps remote contracts explicit."
    if kind == "Exception handling":
        return "Defines service-specific errors or translates exceptions into predictable HTTP responses for clients."
    if kind == "Domain enum":
        return "Defines the finite set of domain states or roles used by entities, DTOs, and business rules."
    if kind == "Utility helper":
        return "Contains reusable stateless logic such as security principal extraction, slug creation, sanitization, or read-time calculation."
    if kind == "Storage abstraction":
        return "Encapsulates storage-specific file operations so media logic can switch between local disk and cloud storage without controller changes."
    if kind == "Package documentation":
        return "Provides package-level Javadoc that explains the service boundary and the role of the root package."
    if kind == "Automated test":
        return "Verifies service behavior, controller delegation, security, exception handling, or integration configuration. It protects the documented behavior from regressions."
    return f"Supports the {rel} implementation area and contributes to the service's runtime or test behavior."


def details(info: FileInfo) -> list[str]:
    result = []
    if info.package:
        result.append(f"Package: {info.package}.")
    if info.classes:
        result.append("Types: " + ", ".join(info.classes) + ".")
    if info.annotations:
        result.append("Important annotations: " + ", ".join("@" + item for item in info.annotations) + ".")
    if info.endpoints:
        result.append("Endpoint annotations: " + "; ".join(info.endpoints) + ".")
    if info.methods:
        result.append("Important methods: " + ", ".join(info.methods) + ".")
    return result


def generate_platform_sections(doc: DocBuilder) -> None:
    doc.add("h1", "InkWell Architecture Overview")
    doc.add("body", "InkWell is a Spring Boot microservices platform for a blogging and publishing product. The backend is organized as independent Maven modules that register with Eureka, route public traffic through Spring Cloud Gateway, expose actuator endpoints to Spring Boot Admin, and use MySQL-backed persistence where a service owns state.")
    doc.add("h2", "Implemented Services")
    for service in SERVICES:
        if service in ACTUAL_SERVICES:
            meta = SERVICE_META[service]
            doc.add("bullet", f"{service}: port {meta['port']}; {meta['summary']}")
        else:
            doc.add("bullet", f"{service}: logical domain only in this repository. {LOGICAL_SERVICE_NOTES[service]}")
    doc.add("h2", "Design Patterns Used")
    for item in [
        "API Gateway Pattern: api-gateway centralizes public routing, CORS, rate limiting, JWT validation, and identity header propagation.",
        "Service Discovery Pattern: discovery-service provides Eureka registration and lookup so services use logical names like lb://auth-service.",
        "Layered Architecture: controllers receive HTTP input, services own business rules, repositories own persistence, and DTOs define API contracts.",
        "DTO Pattern: request/response records decouple public payloads from JPA entities and place validation at the API boundary.",
        "Repository Pattern: Spring Data repositories hide SQL/JPA access behind interfaces.",
        "Event-driven Communication: RabbitMQ routes post, comment, reply, broadcast, and cleanup events across services.",
        "Circuit Breaker Pattern: post-service configures Resilience4j for category-service calls.",
        "Adapter Pattern: media storage and Razorpay/mail integrations are wrapped behind service classes.",
    ]:
        doc.add("bullet", item)
    doc.add("h2", "Overall Platform Communication")
    doc.add("mermaid", """flowchart LR
    Client[React frontend or API client] --> Gateway[api-gateway]
    Gateway --> Eureka[discovery-service]
    Gateway --> Auth[auth-service]
    Gateway --> Post[post-service]
    Gateway --> Comment[comment-service]
    Gateway --> Category[category-service]
    Gateway --> Media[media-service]
    Gateway --> Newsletter[newsletter-service]
    Gateway --> Notify[notification-service]
    Gateway --> Payment[payment-service]
    Auth --> AuthDB[(auth_db)]
    Post --> PostDB[(post_db)]
    Comment --> CommentDB[(comment_db)]
    Category --> CategoryDB[(category_db)]
    Media --> MediaDB[(media_db)]
    Newsletter --> NewsletterDB[(newsletter_db)]
    Notify --> NotifyDB[(notification_db)]
    Post --> Rabbit[(RabbitMQ)]
    Comment --> Rabbit
    Rabbit --> Category
    Rabbit --> Newsletter
    Rabbit --> Notify
    Admin[admin-server] --> Auth
    Admin --> Post
    Admin --> Comment
    Admin --> Category
    Admin --> Media
    Admin --> Newsletter
    Admin --> Notify
    Admin --> Gateway""")
    doc.add("h2", "End-to-End Request Flow")
    for item in [
        "Client sends HTTP request to api-gateway on port 8080.",
        "Gateway route predicates match the path and choose the target service.",
        "For protected routes, JwtAuthenticationFilter validates the bearer token and forwards identity headers such as user id, username, email, role, subscription tier, and subscription status.",
        "Downstream GatewayAuthenticationFilter converts those headers into GatewayUserPrincipal.",
        "Controller validates request DTOs and delegates to the service layer.",
        "Service layer applies business rules and calls repositories, Feign clients, mail/payment adapters, or RabbitMQ.",
        "Repository layer persists or reads service-owned database records.",
        "GlobalExceptionHandler converts validation and domain exceptions into consistent API errors.",
        "Actuator exposes health/metrics to admin-server.",
    ]:
        doc.add("bullet", item)
    doc.add("h2", "Core User Flows")
    flows = {
        "Login/signup flow": "AuthController receives register/login. AuthService validates uniqueness and credentials, PasswordEncoder hashes passwords, JwtService issues access token, RefreshTokenService stores refresh tokens, and ApiResponse returns AuthResponse.",
        "Post creation flow": "Author calls api-gateway /api/posts/author. Gateway validates JWT and role context. PostService sanitizes content, saves Post, syncs taxonomy through CategoryClient, and publishes post.published when published.",
        "Comment flow": "Reader calls comment-service. CommentService validates the post through PostClient, sanitizes content, saves Comment, and publishes comment.created or comment.reply for notification-service.",
        "Subscription/payment flow": "User creates payment order in auth-service or payment-service Razorpay boundary. Razorpay signature is verified, PaymentOrder is updated, subscription tier/status is changed on User, and fresh tokens are issued so gateway sees the new subscription claims.",
        "Notification/newsletter flow": "post.published and comment events enter RabbitMQ. notification-service stores in-app notifications and optional email messages. newsletter-service can send campaigns to active subscribers.",
        "Admin monitoring flow": "Each service registers actuator URLs with admin-server. Operators use the admin UI to see health and metrics while Eureka keeps runtime service discovery separate.",
        "Role-based access flow": "Roles READER, AUTHOR, and ADMIN are encoded in JWTs and propagated by api-gateway. Services enforce endpoint or business-rule checks using GatewayUserPrincipal and Spring Security annotations/configuration.",
    }
    for title, body in flows.items():
        doc.add("h3", title)
        doc.add("body", body)


def add_service_section(doc: DocBuilder, service: str) -> None:
    doc.add("h1", service)
    if service not in ACTUAL_SERVICES:
        doc.add("body", LOGICAL_SERVICE_NOTES[service])
        doc.add("h2", "How to Treat This Domain")
        doc.add("body", "Document, test, and operate this domain through the concrete modules listed above. If a future standalone module is introduced, move the relevant controllers, services, entities, repositories, and routes into that module and update gateway routing.")
        add_logical_diagrams(doc, service)
        return

    meta = SERVICE_META[service]
    doc.add("h2", "Service Purpose")
    doc.add("body", meta["summary"])
    doc.add("bullet", f"Default port: {meta['port']}.")
    doc.add("bullet", f"Database or persistence: {meta['database']}.")
    doc.add("bullet", f"External integrations: {meta['external']}.")

    doc.add("h2", "Request and Internal Flow")
    doc.add("body", meta["flow"])
    doc.add("bullet", "Controller layer: accepts HTTP input, validates DTOs, resolves the principal where needed, and returns ApiResponse or ResponseEntity.")
    doc.add("bullet", "Service layer: owns business rules, authorization decisions, mapping, transactions, event publishing, and remote client calls.")
    doc.add("bullet", "Repository layer: persists service-owned state through Spring Data JPA where the service has a database.")
    doc.add("bullet", "Exception flow: domain exceptions and validation failures are converted by GlobalExceptionHandler where present.")
    doc.add("bullet", f"Authentication/authorization: {meta['security']}")

    doc.add("h2", "Inter-Service Communication")
    doc.add("bullet", "API Gateway: public traffic is routed to this service through api-gateway route predicates.")
    doc.add("bullet", "Eureka: the service registers using spring.application.name so callers can use lb:// names or Feign client names.")
    doc.add("bullet", "JWT propagation: api-gateway validates tokens and forwards identity headers consumed by GatewayAuthenticationFilter when present.")
    doc.add("bullet", "Admin monitoring: actuator endpoints are registered with admin-server for health and metrics.")
    for peer in meta["peers"]:
        doc.add("bullet", f"Communication peer: {peer}.")

    doc.add("h2", "File-by-File Explanation")
    files = collect_files(service)
    for info in files:
        doc.add("h3", info.rel)
        doc.add("body", f"Type: {info.kind}. {file_explanation(info)}")
        for line in details(info):
            doc.add("bullet", line)
        doc.add("bullet", "Why needed: this file keeps its responsibility isolated so the service remains testable and maintainable.")
        doc.add("bullet", "Contribution: it participates in the service's layered flow, configuration, API contract, persistence, security, integration, or regression-test coverage.")

    doc.add("h2", "Service Diagrams")
    add_service_diagrams(doc, service, files)


def add_service_diagrams(doc: DocBuilder, service: str, files: list[FileInfo]) -> None:
    meta = SERVICE_META[service]
    service_label = service.replace("-", " ")
    has_db = meta["database"] != "None"
    has_rabbit = any("Rabbit" in f.kind or "Rabbit" in " ".join(f.annotations) or "RabbitTemplate" in safe_read(f.path) for f in files)
    has_feign = any("FeignClient" in f.annotations for f in files)

    sequence_db = "\n    Service->>Repo: call repository" + ("\n    Repo->>DB: query or save\n    DB-->>Repo: entity data\n    Repo-->>Service: entity result" if has_db else "\n    Repo-->>Service: no database repository in this service")
    remote = ""
    if has_feign:
        remote += "\n    Service->>Remote: call Feign client by service name\n    Remote-->>Service: ApiResponse"
    if has_rabbit:
        remote += "\n    Service->>Rabbit: publish or consume domain event\n    Rabbit-->>Service: async event payload"
    doc.add("h3", "Sequence Diagram")
    doc.add("mermaid", f"""sequenceDiagram
    participant Client
    participant Gateway as api-gateway
    participant Service as {service}
    participant Repo as repository layer
    participant DB as database/storage
    participant Remote as peer service
    participant Rabbit as RabbitMQ
    Client->>Gateway: HTTP request
    Gateway->>Gateway: route match and JWT validation
    Gateway->>Service: forwarded request with identity headers
    Service->>Service: controller validation and service logic{sequence_db}{remote}
    Service-->>Gateway: ApiResponse
    Gateway-->>Client: HTTP response""")

    controllers = type_names(files, "REST controller")[:5]
    services = type_names(files, "Business service")[:5]
    repositories = type_names(files, "Spring Data repository")[:5]
    entities = type_names(files, "JPA entity")[:5]
    clients = type_names(files, "Inter-service client")[:3]
    doc.add("h3", "UML/Class Diagram")
    lines = ["classDiagram"]
    for group in [controllers, services, repositories, entities, clients]:
        for name in group:
            lines.append(f"    class {name}")
    for controller in controllers:
        for svc in services[:1]:
            lines.append(f"    {controller} --> {svc}")
    for svc in services:
        for repo in repositories[:2]:
            lines.append(f"    {svc} --> {repo}")
        for client in clients:
            lines.append(f"    {svc} --> {client}")
    for repo in repositories:
        for entity in entities[:1]:
            lines.append(f"    {repo} --> {entity}")
    if len(lines) == 1:
        lines.append(f"    class {service.replace('-', '_')}")
    doc.add("mermaid", "\n".join(lines))

    doc.add("h3", "Architecture Diagram")
    broker = "    Service --> Rabbit[(RabbitMQ)]\n" if has_rabbit else ""
    db = "    Service --> DB[(Service-owned database)]\n" if has_db else ""
    feign = "    Service --> Peer[Peer service through Feign]\n" if has_feign else ""
    doc.add("mermaid", f"""flowchart LR
    Gateway[api-gateway] --> Service[{service_label}]
    Service --> Eureka[discovery-service]
    Admin[admin-server] --> Service
{db}{feign}{broker}    Service --> Actuator[/actuator health and metrics/]""")

    doc.add("h3", "Service Communication Diagram")
    peer_lines = "\n".join(f"    Service --> {peer.replace('-', '_')}[{peer}]" for peer in meta["peers"])
    doc.add("mermaid", f"""flowchart LR
    Client[Client] --> Gateway[api-gateway]
    Gateway --> Service[{service}]
{peer_lines}
    Service --> Eureka[discovery-service]
    Admin[admin-server] --> Service""")


def add_logical_diagrams(doc: DocBuilder, service: str) -> None:
    if service == "user-service":
        target = "auth-service"
        domain = "user/profile domain"
    else:
        target = "auth-service and payment-service"
        domain = "subscription/payment domain"
    doc.add("h2", "Logical Domain Diagrams")
    doc.add("mermaid", f"""flowchart LR
    Gateway[api-gateway] --> Auth[auth-service]
    Auth --> Domain[{domain}]
    Domain --> DB[(auth_db)]
    Domain --> Payment[payment-service]
    Note[No standalone {service} module in current repo] --> Auth""")
    doc.add("mermaid", f"""sequenceDiagram
    participant Client
    participant Gateway as api-gateway
    participant Target as {target}
    Client->>Gateway: request for {domain}
    Gateway->>Target: route to existing module
    Target-->>Gateway: response
    Gateway-->>Client: response""")


def type_names(files: list[FileInfo], kind: str) -> list[str]:
    names = []
    for info in files:
        if info.kind == kind:
            names.extend(info.classes or [Path(info.rel).stem])
    return unique(names)


def safe_read(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        return ""


def build_document() -> DocBuilder:
    doc = DocBuilder()
    doc.add("title", "InkWell Microservices Platform Technical Documentation")
    doc.add("body", "Generated from the local backend source tree. Build artifacts and frontend node_modules are excluded. Secrets from configuration files are intentionally not printed.")
    doc.add("body", "Scope: backend architecture, service-by-service file explanations, communication flows, security model, database interactions, exception handling, DTO/entity mapping, messaging, monitoring, and Mermaid diagrams.")
    generate_platform_sections(doc)
    for service in SERVICES:
        add_service_section(doc, service)
    doc.add("h1", "Final Summary")
    doc.add("body", "InkWell follows a layered microservice architecture with api-gateway as the edge, discovery-service as the service registry, admin-server as the monitoring console, and independently persisted domain services. Authentication and user/subscription state currently live in auth-service; post, comment, category, media, newsletter, notification, and payment concerns are split into dedicated modules. RabbitMQ provides asynchronous event flow, Feign provides typed synchronous service calls, and actuator/Eureka/Admin Server provide runtime observability.")
    doc.add("body", "The added package-info.java files document service package boundaries without changing business logic. This PDF and its Markdown source provide the full beginner-friendly but technically detailed reference for maintainers.")
    return doc


STYLE = {
    "title": {"size": 20, "leading": 28, "space_before": 20, "space_after": 12},
    "h1": {"size": 15, "leading": 20, "space_before": 16, "space_after": 6},
    "h2": {"size": 12, "leading": 16, "space_before": 10, "space_after": 4},
    "h3": {"size": 10, "leading": 13, "space_before": 6, "space_after": 2},
    "body": {"size": 8.5, "leading": 11, "space_before": 2, "space_after": 2},
    "bullet": {"size": 8.2, "leading": 10.5, "space_before": 1, "space_after": 1},
    "code": {"size": 7.0, "leading": 9, "space_before": 4, "space_after": 4},
    "mermaid": {"size": 7.0, "leading": 9, "space_before": 4, "space_after": 4},
}


def paginate(blocks: list[dict[str, str]], start_page: int) -> tuple[list[list[tuple[str, str]]], list[tuple[str, int]]]:
    pages: list[list[tuple[str, str]]] = [[]]
    y = 748.0
    toc: list[tuple[str, int]] = []
    page_no = start_page

    def new_page() -> None:
        nonlocal y, page_no
        pages.append([])
        y = 748.0
        page_no += 1

    for block in blocks[1:]:
        style = block["style"]
        text = block["text"]
        if style == "title":
            continue
        if style == "h1":
            if pages[-1]:
                new_page()
            toc.append((text, page_no))
        spec = STYLE.get(style, STYLE["body"])
        y -= spec["space_before"]
        if y < 70:
            new_page()
        lines = wrap_for_pdf(text, style)
        if style == "bullet":
            lines = [("- " + lines[0])] + [("  " + line) for line in lines[1:]] if lines else []
        if style in {"code", "mermaid"}:
            lines = ["    " + line for line in lines]
        for line in lines or [""]:
            if y < 54:
                new_page()
            pages[-1].append((line, style))
            y -= spec["leading"]
        y -= spec["space_after"]
    return pages, toc


def wrap_for_pdf(text: str, style: str) -> list[str]:
    if not text:
        return [""]
    width = 86
    if style == "h1":
        width = 56
    elif style == "h2":
        width = 68
    elif style == "h3":
        width = 78
    elif style in {"code", "mermaid"}:
        width = 108
        lines: list[str] = []
        for raw in text.splitlines():
            if len(raw) <= width:
                lines.append(raw)
            else:
                lines.extend(textwrap.wrap(raw, width=width, replace_whitespace=False, drop_whitespace=False))
        return lines
    return textwrap.wrap(text, width=width, break_long_words=False, replace_whitespace=False) or [""]


def pdf_escape(text: str) -> str:
    text = clean(text)
    return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")


def make_pdf(doc: DocBuilder) -> None:
    content_pages, toc = paginate(doc.blocks, 3)
    pages: list[list[tuple[str, str]]] = []
    pages.append(cover_page())
    pages.extend(toc_pages(toc))
    pages.extend(content_pages)
    write_pdf(pages, OUT_PDF)


def cover_page() -> list[tuple[str, str]]:
    return [
        ("InkWell Microservices Platform", "cover_title"),
        ("Complete Technical Documentation", "cover_subtitle"),
        ("Backend services, communication flows, security, persistence, messaging, monitoring, and file-by-file explanations.", "cover_body"),
        ("Generated artifact: docs/InkWell-Technical-Documentation.pdf", "cover_body"),
    ]


def toc_pages(toc: list[tuple[str, int]]) -> list[list[tuple[str, str]]]:
    page = [("Table of Contents", "toc_title")]
    for title, page_no in toc:
        page.append((f"{title} .... {page_no}", "toc"))
    return [page]


def write_pdf(pages: list[list[tuple[str, str]]], output: Path) -> None:
    objects: dict[int, bytes] = {}
    font_id = 3
    page_ids = []
    next_id = 4
    for page in pages:
        page_id = next_id
        content_id = next_id + 1
        next_id += 2
        page_ids.append(page_id)
        stream = render_page_stream(page)
        objects[content_id] = b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream"
        objects[page_id] = f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 {font_id} 0 R >> >> /Contents {content_id} 0 R >>".encode("ascii")
    objects[1] = b"<< /Type /Catalog /Pages 2 0 R >>"
    kids = " ".join(f"{page_id} 0 R" for page_id in page_ids)
    objects[2] = f"<< /Type /Pages /Kids [{kids}] /Count {len(page_ids)} >>".encode("ascii")
    objects[3] = b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"

    ordered_ids = sorted(objects)
    data = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = {0: 0}
    for obj_id in ordered_ids:
        offsets[obj_id] = len(data)
        data.extend(f"{obj_id} 0 obj\n".encode("ascii"))
        data.extend(objects[obj_id])
        data.extend(b"\nendobj\n")
    xref_pos = len(data)
    size = max(ordered_ids) + 1
    data.extend(f"xref\n0 {size}\n".encode("ascii"))
    data.extend(b"0000000000 65535 f \n")
    for obj_id in range(1, size):
        data.extend(f"{offsets.get(obj_id, 0):010d} 00000 n \n".encode("ascii"))
    data.extend(f"trailer\n<< /Size {size} /Root 1 0 R >>\nstartxref\n{xref_pos}\n%%EOF\n".encode("ascii"))
    output.write_bytes(data)


def render_page_stream(page: list[tuple[str, str]]) -> bytes:
    y = 748.0
    parts = []
    for text, style in page:
        size, leading, x = pdf_style(style)
        if y < 48:
            break
        parts.append(f"BT /F1 {size:.1f} Tf {x:.1f} {y:.1f} Td ({pdf_escape(text)}) Tj ET")
        y -= leading
    return ("\n".join(parts)).encode("ascii")


def pdf_style(style: str) -> tuple[float, float, float]:
    if style == "cover_title":
        return 24.0, 34.0, 72.0
    if style == "cover_subtitle":
        return 16.0, 26.0, 72.0
    if style == "cover_body":
        return 10.0, 16.0, 72.0
    if style == "toc_title":
        return 18.0, 28.0, 72.0
    if style == "toc":
        return 10.0, 15.0, 86.0
    spec = STYLE.get(style, STYLE["body"])
    x = 54.0
    if style == "bullet":
        x = 72.0
    if style in {"code", "mermaid"}:
        x = 66.0
    return float(spec["size"]), float(spec["leading"]), x


def main() -> None:
    OUT_MD.parent.mkdir(parents=True, exist_ok=True)
    doc = build_document()
    OUT_MD.write_text("".join(doc.md), encoding="utf-8")
    make_pdf(doc)
    print(f"Wrote {OUT_MD}")
    print(f"Wrote {OUT_PDF}")


if __name__ == "__main__":
    main()
