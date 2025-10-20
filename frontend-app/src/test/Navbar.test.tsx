import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { Navbar } from "@/components/Navbar";
import { beforeEach, describe, expect, it } from "vitest";

const renderNavbar = () => {
  render(
    <MemoryRouter>
      <Navbar />
    </MemoryRouter>
  );
};

describe("Navbar render", () => {
    beforeEach(() => {
        sessionStorage.clear();
      });
  it("renders logo and site title", () => {
    renderNavbar();
    expect(screen.getByAltText(/Green & Tasty logo/i)).toBeInTheDocument();
    expect(screen.getByText(/green & tasty/i)).toBeInTheDocument();
  });
  it("renders guest navigation links when not logged in", () => {
    renderNavbar();
    expect(screen.getByText(/main page/i)).toBeInTheDocument();
    expect(screen.getByText(/book a table/i)).toBeInTheDocument();
    expect(screen.getByText(/sign in/i)).toBeInTheDocument();
  });
  it("renders waiter navigation links when role is Waiter", () => {
    sessionStorage.setItem("token", "abc");
    sessionStorage.setItem("username", "waiter1");
    sessionStorage.setItem("role", "Waiter");
    renderNavbar();
    expect(screen.getByText(/reservation/i)).toBeInTheDocument();
    expect(screen.getByText(/menu/i)).toBeInTheDocument();
  });
  it('renders links for a logged-in user with non-waiter role', () => {
    sessionStorage.setItem('token', 'abc');
    sessionStorage.setItem('username', 'user1');
    sessionStorage.setItem('role', 'Customer');
    renderNavbar();
    expect(screen.getByText(/reservation/i)).toBeInTheDocument();
    expect(screen.getByText(/main page/i)).toBeInTheDocument();
  });
  it('toggles mobile menu', () => {
    renderNavbar();
    const menuButton = screen.getByTestId('hamburger');
    fireEvent.click(menuButton);
    expect(screen.queryAllByText(/main page/i)[0]).toBeInTheDocument();
  });
  it("shows mobile menu and Sign In button when not logged in", () => {
    renderNavbar();
    const menuButton = screen.getByTestId("hamburger");
    fireEvent.click(menuButton);

    expect(screen.getAllByText(/main page/i)[0]).toBeInTheDocument();
    expect(screen.getAllByText(/Book a Table/i)[0]).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /Sign In/i })[0]).toBeInTheDocument();
  });

  it("shows mobile menu without Sign In button when logged in", () => {
    sessionStorage.setItem("token", "abc");
    sessionStorage.setItem("username", "john");
    sessionStorage.setItem("role", "Customer");
    renderNavbar();

    const menuButton = screen.getByRole("button");
    fireEvent.click(menuButton);

    expect(screen.getAllByText(/main page/i)[0]).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /sign in/i })).not.toBeInTheDocument();
  });

  it('shows user dropdown and handles logout', () => {
    sessionStorage.setItem('token', 'abc');
    sessionStorage.setItem('username', 'john');
    sessionStorage.setItem('role', 'Customer');
    renderNavbar();
  
    const avatar = screen.getByTestId('profileDropdown') || screen.getByRole('img'); // or get by class
    fireEvent.click(avatar);
  
    expect(screen.getByText(/john/i)).toBeInTheDocument();
    fireEvent.click(screen.getByText(/sign out/i));
  
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(screen.getByText(/sign in/i)).toBeInTheDocument();
  });
  it('closes dropdown on outside click', () => {
    sessionStorage.setItem('token', 'abc');
    sessionStorage.setItem('username', 'john');
    sessionStorage.setItem('role', 'Customer');
    renderNavbar();
  
    const avatar = screen.getByTestId('profileDropdown');
    fireEvent.click(avatar);
  
    expect(screen.getByText(/john/i)).toBeInTheDocument();
  
    fireEvent.mouseDown(document);
    expect(screen.queryByText(/john/i)).not.toBeInTheDocument();
  });
});
