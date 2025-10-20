
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter as Router } from 'react-router-dom';
import { toast } from 'react-toastify';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import Registration from '@/pages/Signup';

vi.mock('@/api/registrationService', () => ({
  registrationService: vi.fn(() => Promise.resolve({ message: 'Registration successful' })),
}));

vi.mock('react-toastify', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('Registration Component', () => {
  beforeEach(() => {
    render(
      <Router>
        <Registration />
      </Router>
    );
  });

  it('renders the registration form', () => {
    expect(screen.getByTestId(/title/i)).toBeInTheDocument();
  });

  it('validates form fields', async () => {
    fireEvent.click(screen.getByRole('button', { name: /Create an Account/i }));

    expect(screen.getByText(/First name can be up to 50 characters/i)).toBeInTheDocument();
    expect(screen.getByText(/Invalid email address/i)).toBeInTheDocument();
  });

  it('toggles password visibility', () => {
    const passwordInput = screen.getByPlaceholderText(/Enter Password/i);
    const toggleButton = screen.getByTestId(/togglePassword/i);

    expect(passwordInput).toHaveAttribute('type', 'password');
    fireEvent.click(toggleButton);
    expect(passwordInput).toHaveAttribute('type', 'text');
  });

  it("shows the message when cofirmPassword is empty", () => {
    fireEvent.change(screen.getByPlaceholderText(/Enter Password/i), {
      target: {value : "Password123!"}
    });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), {
      target: {value: ""},
    })
    expect(screen.getByText(/Must match the password/i)).toBeInTheDocument();
  })

  it('submits the form successfully', async () => {
    fireEvent.change(screen.getByPlaceholderText(/Enter First Name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByPlaceholderText(/Enter Last Name/i), { target: { value: 'Doe' } });
    fireEvent.change(screen.getByPlaceholderText(/Enter Email/i), { target: { value: 'john.doe@example.com' } });
    fireEvent.change(screen.getByPlaceholderText(/Enter Password/i), { target: { value: 'Password123!' } });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: 'Password123!' } });

    fireEvent.click(screen.getByRole('button', { name: /Create an Account/i }));
    await vi.waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith("Registration successful");
    })
  });
});
