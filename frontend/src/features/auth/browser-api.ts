import { ApiRequestError, apiRequest } from "../../lib/api.ts";

export type Account = { id: string; role: "STUDENT" | "TEACHER" | "ADMIN"; emailVerified: boolean };
export type Session = { accessToken: string; account: Account };
export type Onboarding = {
  applicationId: string; status: "PENDING" | "APPROVED" | "REJECTED";
  submittedAt: string; reviewedAt: string | null; rejectionReason: string | null;
};

export async function authMutation<T>(path: string, body?: Record<string, string>, accessToken?: string): Promise<T | undefined> {
  const csrf = await apiRequest<{ token: string }>("/auth/csrf", { credentials: "include", cache: "no-store" });
  if (!csrf?.token) throw new Error("Invalid CSRF response");
  return apiRequest<T>(path, {
    method: "POST", credentials: "include", cache: "no-store",
    headers: { "X-XSRF-TOKEN": csrf.token, ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}), ...(body ? { "Content-Type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
}

export function authorized<T>(token: string, path: string, body?: Record<string, string>, method = body ? "POST" : "GET") {
  return apiRequest<T>(path, {
    method, credentials: "omit", cache: "no-store",
    headers: { Authorization: `Bearer ${token}`, ...(body ? { "Content-Type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
}

export async function signIn(body?: Record<string, string>): Promise<Session> {
  const result = await authMutation<{ accessToken: string; tokenType: string }>(body ? "/auth/login" : "/auth/refresh", body);
  if (!result?.accessToken || result.tokenType !== "Bearer") throw new Error("Invalid authentication response");
  const account = await authorized<Account>(result.accessToken, "/users/me");
  if (!account || !["STUDENT", "TEACHER", "ADMIN"].includes(account.role)) throw new Error("Invalid account response");
  return { accessToken: result.accessToken, account };
}

export function safeMessage(error: unknown): string {
  if (!(error instanceof ApiRequestError)) return "Chưa xác nhận được kết quả. Vui lòng kiểm tra kết nối trước khi tiếp tục. Yêu cầu không được tự động gửi lại.";
  const messages: Record<string, string> = {
    EMAIL_VERIFICATION_REQUIRED: "Vui lòng xác minh email trước khi đăng nhập.",
    INVALID_RESET_CREDENTIAL: "Mã đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng. Vui lòng yêu cầu mã mới.",
    INVALID_VERIFICATION_CREDENTIAL: "Mã xác minh không hợp lệ, đã hết hạn hoặc đã được sử dụng. Vui lòng yêu cầu mã mới nếu cần.",
    REGISTRATION_CONFLICT: "Thông tin đăng ký xung đột với tài khoản đã có. Vui lòng kiểm tra email và tên người dùng.",
    APPLICATION_ALREADY_REVIEWED: "Hồ sơ này đã được xét duyệt. Vui lòng kiểm tra trạng thái mới nhất.",
    TEACHER_PROFILE_CONFLICT: "Hồ sơ gia sư đã tồn tại. Không thể phê duyệt đơn đăng ký này.",
    APPLICATION_NOT_REVIEWABLE: "Hồ sơ này không còn đủ điều kiện xét duyệt.",
    VERIFICATION_RATE_LIMITED: "Vui lòng chờ ít nhất 60 giây giữa các yêu cầu gửi lại. Chỉ chấp nhận tối đa 5 lần gửi lại trong mỗi khoảng 24 giờ liên tiếp.",
    VERIFICATION_DELIVERY_UNAVAILABLE: "Không gửi được email. Tài khoản của bạn vẫn được giữ lại. Vui lòng yêu cầu gửi lại sau thời gian chờ.",
    AUTHENTICATION_OUTCOME_UNKNOWN: "Chưa xác định được kết quả. Không tự động gửi lại yêu cầu. Vui lòng kiểm tra trạng thái tài khoản trước khi thử lại.",
  };
  return messages[error.code] ?? ({
    400: "Vui lòng kiểm tra thông tin đã nhập và thử lại.",
    401: "Bạn cần đăng nhập hoặc phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
    403: "Tài khoản hiện tại không được phép thực hiện thao tác này.",
    404: "Không tìm thấy hồ sơ mà bạn được phép truy cập. Không thể tạo đơn đăng ký ban đầu riêng tại đây.",
    409: "Trạng thái hồ sơ đã thay đổi hoặc không được phép gửi lại. Vui lòng kiểm tra trạng thái mới nhất.",
    500: "Máy chủ không thể hoàn tất yêu cầu an toàn.",
    503: "Dịch vụ tạm thời không khả dụng hoặc chưa xác nhận được kết quả.",
  } as Record<number, string>)[error.status] ?? "Không thể hoàn tất yêu cầu.";
}

export function validPassword(password: string) {
  for (let i = 0; i < password.length; i++) {
    const code = password.charCodeAt(i);
    if (code >= 0xd800 && code <= 0xdbff) {
      const next = password.charCodeAt(++i);
      if (!(next >= 0xdc00 && next <= 0xdfff)) return false;
    } else if (code >= 0xdc00 && code <= 0xdfff) return false;
  }
  return Array.from(password).length >= 8 && Array.from(password).length <= 64 && new TextEncoder().encode(password).length <= 72;
}
