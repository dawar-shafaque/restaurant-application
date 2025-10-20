import { API } from "@/api/endpoints";
import { loginService } from "@/api/loginService";
import { toast } from "react-toastify";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("react-toastify", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

vi.mock("@/api/endpoints", () => ({
  API: {
    LOGIN_API: `${import.meta.env.VITE_BASE_URLL}${import.meta.env.VITE_LOGIN_API}`,
  },
}));

describe("loginService", () => {
  const mockUserData = {
    email: "test@example.com",
    password: "password123",
  };

  beforeEach(() => {
    // Reset mocks between test cases
    vi.resetAllMocks();
  });

  it("should make post request to login api and return response data on success", async () => {
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            success: true,
            message: "Login successful",
            token: "fake-token",
          }),
      } as Response)
    );
    const response = await loginService(mockUserData);
    expect(global.fetch).toHaveBeenCalledWith(API.LOGIN_API, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(mockUserData),
    });
    expect(response).toEqual({
      success: true,
      message: "Login successful",
      token: "fake-token",
    });
    expect(toast.error).not.toHaveBeenCalled();
  });
  it("should show an error toast when API returns an error response", async () => {
    // Mock the fetch API for an error response
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: false,
        json: () =>
          Promise.resolve({
            message: "Invalid email or password",
          }),
        status: 401,
        text: () => Promise.resolve("Unauthorized"),
      } as Response)
    );

    // Expect the service to throw an error
    await expect(loginService(mockUserData)).rejects.toThrow(
      "Network error or API issue."
    );

    // Assert toast.error is called with the correct message
    expect(toast.error).toHaveBeenCalledWith("Invalid email or password");
  });
  it("should handle network errors gracefully", async () => {
    // Mock the fetch API for a network error
    global.fetch = vi.fn(() => Promise.reject(new Error("Network error")));

    // Expect the service to throw a Network error
    await expect(loginService(mockUserData)).rejects.toThrow(
      "Network error or API issue."
    );

    // Ensure no toast errors are shown
    expect(toast.error).not.toHaveBeenCalled();
  });
});
