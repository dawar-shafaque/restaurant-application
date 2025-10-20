import { API } from "@/api/endpoints";
import { registrationService } from "@/api/registrationService";
import { vi, it, describe, expect, beforeEach } from "vitest";

vi.mock("@/api/endpoints", () => ({
  API: {
    SIGNUP_API: `${import.meta.env.VITE_BASE_URL}${import.meta.env.VITE_SIGNUP_API}`,
  },
}));

describe("registrationService", () => {
  const mockUserData = {
    firstName: "John",
    lastName: "Doe",
    email: "test@example.com",
    password: "password123",
  };

  beforeEach(() => {
    vi.resetAllMocks();
    vi.clearAllMocks();
  });

  it("should make a POST request and return parsed JSON on success", async () => {
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        clone: () => ({
          text: () =>
            Promise.resolve(
              JSON.stringify({
                success: true,
                message: "Registration successful",
              })
            ),
        }),
        status: 200,
      } as Response)
    );

    const response = await registrationService(mockUserData);

    expect(global.fetch).toHaveBeenCalledWith(API.SIGNUP_API, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(mockUserData),
    });

    expect(response).toEqual({
      success: true,
      message: "Registration successful",
    });
  });

  it("should return fallback text if response is not valid JSON", async () => {
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        clone: () => ({
          text: () => Promise.resolve("Registration successful"),
        }),
        status: 200,
      } as Response)
    );

    const response = await registrationService(mockUserData);

    expect(response).toEqual({
      message: "Registration successful",
      e: expect.anything(), // Parsing error object
    });
  });

  it("should throw an error if the API returns a non-OK response", async () => {
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: false,
        clone: () => ({
          text: () => Promise.resolve("Internal Server Error"),
        }),
        status: 500,
      } as Response)
    );

    await expect(registrationService(mockUserData)).rejects.toThrow(
      "Network error or API issue."
    );
  });
});
