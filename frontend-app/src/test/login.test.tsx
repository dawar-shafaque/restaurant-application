import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Login from "../pages/Login"; // adjust the path if needed
import { vi, expect, it, describe, beforeEach, Mock } from "vitest";
import { loginService } from "@/api/loginService";

// Mock react-toastify
vi.mock("react-toastify", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

// Mock loginService
vi.mock("@/api/loginService", () => ({
  loginService: vi.fn(),
}));

// Mock useNavigate
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => vi.fn(),
  };
});

describe("Login Component", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it("renders email and password inputs", () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(screen.getByPlaceholderText(/enter email/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/enter password/i)).toBeInTheDocument();
    expect(screen.getByRole("button",{name: /Sign In/i})).toBeInTheDocument();
  });

  it("shows validation errors when submitting empty form", async () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    expect(await screen.findByText(/email address is required/i)).toBeInTheDocument();
    expect(await screen.findByText(/password is required/i)).toBeInTheDocument();
  });

  it("submits form successfully and stores token", async () => {
    (loginService as Mock).mockResolvedValue({
      accessToken: "mock-token",
      role: "Waiter",
      name: "mockuser",
    });

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByPlaceholderText(/enter email/i), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText(/enter password/i), {
      target: { value: "password123" },
    });

    fireEvent.click(screen.getByRole("button", { name: /Sign In/i }));

    await waitFor(() => {
      expect(sessionStorage.getItem("token")).toBe("mock-token");
      expect(sessionStorage.getItem("role")).toBe("Waiter");
      expect(sessionStorage.getItem("username")).toBe("mockuser");
    });
  });

  it("toggles password visibility", () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    const passwordInput = screen.getByPlaceholderText(/enter password/i);
    const toggleButton = screen.getByTestId(/togglePassword/i);

    // Initially type should be password
    expect(passwordInput).toHaveAttribute("type", "password");

    fireEvent.click(toggleButton);

    // After click, type should be text
    expect(passwordInput).toHaveAttribute("type", "text");
  });
});
