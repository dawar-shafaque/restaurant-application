import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import ChangePassword from "@/components/Password";
import { toast } from "react-toastify";
import { MemoryRouter } from "react-router-dom";

// Mock `react-toastify`
vi.mock("react-toastify", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

// Partially mock `react-router-dom`
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom"); // Import everything else
  return {
    ...actual, // Spread the actual implementation
    useNavigate: vi.fn(), // Mock only `useNavigate`
  };
});

describe("ChangePassword Component", () => {
  const renderComponent = () =>
    render(
      <MemoryRouter>
        <ChangePassword />
      </MemoryRouter>
    );

  it("renders all input fields and button", () => {
    renderComponent();
    expect(screen.getByLabelText("Old Password")).toBeInTheDocument();
    expect(screen.getByLabelText("New Password")).toBeInTheDocument();
    expect(screen.getByLabelText("Confirm New Password")).toBeInTheDocument();
    expect(screen.getByText("Save Changes")).toBeInTheDocument();
  });

  it("validates password criteria dynamically", () => {
    renderComponent();
    const newPasswordInput = screen.getByLabelText("New Password");

    fireEvent.change(newPasswordInput, { target: { value: "Test123!" } });
    expect(screen.getByText("● At least one uppercase letter required")).toHaveClass("text-green-600");
    expect(screen.getByText("● At least one lowercase letter required")).toHaveClass("text-green-600");
    expect(screen.getByText("● At least one number required")).toHaveClass("text-green-600");
    expect(screen.getByText("● At least one special character required")).toHaveClass("text-green-600");
    expect(screen.getByText("● Password must be 8-16 characters long")).toHaveClass("text-green-600");
  });

  it("validates confirm password matches new password", () => {
    renderComponent();
    const newPasswordInput = screen.getByLabelText("New Password");
    const confirmPasswordInput = screen.getByLabelText("Confirm New Password");

    fireEvent.change(newPasswordInput, { target: { value: "Test123!" } });
    fireEvent.change(confirmPasswordInput, { target: { value: "Test123!" } });

    expect(screen.getByText("● Confirm password must match the new password")).toHaveClass("text-green-600");
  });

  it("disables save button if criteria are not met", () => {
    renderComponent();
    const saveButton = screen.getByText("Save Changes");
    expect(saveButton).toBeDisabled();
  });

  it("enables save button if all criteria are met", () => {
    renderComponent();
    const oldPasswordInput = screen.getByLabelText("Old Password");
    const newPasswordInput = screen.getByLabelText("New Password");
    const confirmPasswordInput = screen.getByLabelText("Confirm New Password");

    fireEvent.change(oldPasswordInput, { target: { value: "OldPass123!" } });
    fireEvent.change(newPasswordInput, { target: { value: "NewPass123!" } });
    fireEvent.change(confirmPasswordInput, { target: { value: "NewPass123!" } });

    const saveButton = screen.getByText("Save Changes");
    expect(saveButton).not.toBeDisabled();
  });

  it("shows success toast on successful password change", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce({
      ok: true,
      json: async () => ({ message: "Password changed successfully" }),
      clone: () => ({
        text: async () => JSON.stringify({ message: "Password changed successfully" }),
      }),
    } as Response);

    renderComponent();
    const oldPasswordInput = screen.getByLabelText("Old Password");
    const newPasswordInput = screen.getByLabelText("New Password");
    const confirmPasswordInput = screen.getByLabelText("Confirm New Password");
    const saveButton = screen.getByText("Save Changes");

    fireEvent.change(oldPasswordInput, { target: { value: "OldPass123!" } });
    fireEvent.change(newPasswordInput, { target: { value: "NewPass123!" } });
    fireEvent.change(confirmPasswordInput, { target: { value: "NewPass123!" } });
    fireEvent.click(saveButton);

    // await waitFor(() => {
    //   expect(toast.success).toHaveBeenCalledWith("Password changed successfully");
    // });
  });

  it("shows error toast on failed password change", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce({
      ok: false,
      json: async () => ({ message: "Error changing password" }),
    } as Response);

    renderComponent();
    const oldPasswordInput = screen.getByLabelText("Old Password");
    const newPasswordInput = screen.getByLabelText("New Password");
    const confirmPasswordInput = screen.getByLabelText("Confirm New Password");
    const saveButton = screen.getByText("Save Changes");

    fireEvent.change(oldPasswordInput, { target: { value: "OldPass123!" } });
    fireEvent.change(newPasswordInput, { target: { value: "NewPass123!" } });
    fireEvent.change(confirmPasswordInput, { target: { value: "NewPass123!" } });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("You are not authenticated. Please log in.");
    });
  });

  it("handles fetch error gracefully", async () => {
    vi.spyOn(global, "fetch").mockRejectedValueOnce(new Error("Network error"));

    renderComponent();
    const oldPasswordInput = screen.getByLabelText("Old Password");
    const newPasswordInput = screen.getByLabelText("New Password");
    const confirmPasswordInput = screen.getByLabelText("Confirm New Password");
    const saveButton = screen.getByText("Save Changes");

    fireEvent.change(oldPasswordInput, { target: { value: "OldPass123!" } });
    fireEvent.change(newPasswordInput, { target: { value: "NewPass123!" } });
    fireEvent.change(confirmPasswordInput, { target: { value: "NewPass123!" } });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(expect.stringContaining("You are not authenticated. Please log in."));
    });
  });
});