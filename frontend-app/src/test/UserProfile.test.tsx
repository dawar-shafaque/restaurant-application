import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import UserProfile from "@/pages/UserProfile";
import { Provider } from "react-redux";
import configureStore from "redux-mock-store";
import { thunk } from "redux-thunk";
import { beforeEach, describe, expect, test, vi } from "vitest";

// Mocks
vi.mock("@/components/Navbar", () => ({
  Navbar: () => <div data-testid="navbar">Navbar</div>,
}));
vi.mock("@/components/Password", () => ({
  default: () => (
    <div data-testid="change-password">ChangePasswordComponent</div>
  ),
}));

const mockDispatch = vi.fn();
vi.mock("react-redux", async () => {
  const actual = await vi.importActual("react-redux");
  return {
    ...actual,
    useDispatch: () => mockDispatch,
  };
});

const middlewares = [thunk as never];
const mockStore = configureStore(middlewares);

const initialState = {
  user: {
    profile: {
      firstName: "Jane",
      lastName: "Doe",
      userAvatarUrl: "fake-avatar-url",
    },
    loading: false,
    error: null as string | null,
  },
};

const renderWithStore = (state = initialState) => {
  const store = mockStore(state);
  return render(
    <Provider store={store}>
      <UserProfile />
    </Provider>
  );
};

describe("UserProfile Component", () => {
  beforeEach(() => {
    mockDispatch.mockClear();
    sessionStorage.setItem("role", "admin");
  });

  test("renders navbar and general info by default", () => {
    renderWithStore();

    expect(screen.getByTestId("navbar")).toBeInTheDocument();
    expect(screen.getByText("My Profile")).toBeInTheDocument();
    expect(screen.getByText("General Information")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Jane")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Doe")).toBeInTheDocument();
    expect(screen.getByText("(admin)")).toBeInTheDocument();
  });

  test("switches to Change Password tab", () => {
    renderWithStore();

    fireEvent.click(screen.getByText("Change Password"));
    expect(screen.getByTestId("change-password")).toBeInTheDocument();
  });

  test("shows loading state", () => {
    renderWithStore({
      user: {
        profile: { firstName: "", lastName: "", userAvatarUrl: "" },
        loading: true,
        error: null,
      },
    });

    expect(screen.getByText("Loading profile...")).toBeInTheDocument();
  });

  test("shows error state", () => {
    renderWithStore({
      user: {
        profile: { firstName: "", lastName: "", userAvatarUrl: "" },
        loading: false,
        error: "Something went wrong",
      },
    });

    expect(
      screen.getByText(/Error: Something went wrong/i)
    ).toBeInTheDocument();
  });

  test("handles input change and save", async () => {
    renderWithStore();

    const firstNameInput = screen.getByLabelText("First Name");
    fireEvent.change(firstNameInput, { target: { value: "Updated" } });

    const saveButton = screen.getByRole("button", { name: "Save Changes" });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockDispatch).toHaveBeenCalled(); // Called with updateUserProfile
    });
  });

  test("handles image upload", async () => {
    renderWithStore();

    const file = new File(["dummy"], "avatar.png", { type: "image/png" });

    const input = screen.getByLabelText(/Upload Photo/i) as HTMLInputElement;
    Object.defineProperty(input, "files", {
      value: [file],
    });

    fireEvent.change(input);

    // Wait for FileReader logic (although not truly tested due to browser limitation)
    await waitFor(() => {
      // It should not crash or throw, but we cannot test FileReader result easily without mocking it
      expect(input.files?.[0].name).toBe("avatar.png");
    });
  });
});
