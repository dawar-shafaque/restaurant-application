import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import React, { useState } from "react";
import Logo from "../assets/vegetarian-_1_ 1.svg";
import { Link, useNavigate } from "react-router-dom";
import { registrationService } from "@/api/registrationService";
import { toast } from "react-toastify";
import { Eye, EyeOff } from "lucide-react";

interface formType {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

const Registration: React.FC = () => {
  // Form Data State
  const navigate = useNavigate();
  const [formData, setFormData] = useState<formType>({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Error State
  const [errors, setErrors] = useState({
    firstName: false,
    lastName: false,
    email: false,
    password: false,
    confirmPassword: false,
  });

  // Password Rules State
  const [passwordRules, setPasswordRules] = useState({
    hasUppercase: false,
    hasLowercase: false,
    hasNumber: false,
    hasSpecialChar: false,
    isValidLength: false,
  });

  const validateName = (name: string) => {
    const nameRegex = /^[A-Za-zÀ-ÖØ-öø-ÿ'’-]{1,50}$/;
    return nameRegex.test(name);
  };
  // Handle Input Change
  const handleChange = (field: keyof typeof formData, value: string) => {
    setFormData({
      ...formData,
      [field]: value,
    });

    // Clear error when input is corrected
    setErrors((prevErrors) => ({
      ...prevErrors,
      [field]: false,
    }));

    // Validate firstName and lastName on input
    if (field === "firstName" || field === "lastName") {
      setErrors((prevErrors) => ({
        ...prevErrors,
        [field]: !validateName(value),
      }));
    } else {
      // Clear error when input is corrected
      setErrors((prevErrors) => ({
        ...prevErrors,
        [field]: false,
      }));
    }
    // If password is being updated, validate its rules
    if (field === "password") {
      handlePasswordValidation(value);
    }
  };

  // Validate Password Rules
  const handlePasswordValidation = (password: string) => {
    setPasswordRules({
      hasUppercase: /[A-Z]/.test(password), // At least one uppercase letter
      hasLowercase: /[a-z]/.test(password), // At least one lowercase letter
      hasNumber: /\d/.test(password), // At least one number
      hasSpecialChar: /[@$!%*?&#]/.test(password), // At least one special character
      isValidLength: password.length >= 8 && password.length <= 16, // 8-16 characters
    });
  };

  // Handle Submit
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const { firstName, lastName, email, password, confirmPassword } = formData;
    const nameRegex = /^[A-Za-zÀ-ÖØ-öø-ÿ'’-]{1,50}$/;
    // Validation Logic
    const newErrors = {
      firstName: !firstName.trim() || !nameRegex.test(firstName),
      lastName: !lastName.trim() || !nameRegex.test(lastName),
      email: !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email), // Basic email format validation
      password: !(
        passwordRules.hasUppercase &&
        passwordRules.hasLowercase &&
        passwordRules.hasNumber &&
        passwordRules.hasSpecialChar &&
        passwordRules.isValidLength
      ), // Ensure all rules are met
      confirmPassword: password !== confirmPassword || !confirmPassword.trim(),
    };

    setErrors(newErrors);
    // Check if any errors exist
    if (Object.values(newErrors).some((error) => error)) {
      return;
    }

    const requestData = { firstName, lastName, email, password };
    console.log(requestData)
    try {
      const responseData = await registrationService(requestData);
      console.log(responseData);
      toast.success(responseData.message);
      navigate("/login");
    } catch (error) {
      toast.error("Registration failed. Please check your details.");
      console.error("Error : ", error);
    }
  };
  return (
    <div className="w-full min-h-screen flex items-center justify-center bg-neutral-100 p-10">
      {/* Container */}
      <div className="w-full h-full flex flex-col md:flex-row bg-neutral-100 rounded-lg overflow-hidden">
        {/* Left Section - Form */}
        <div className="flex flex-col w-full md:w-1/2 h-auto md:h-full p-6 sm:p-12">
          <p className="text-xs sm:text-sm text-gray-500 mb-3">
            LET’S GET YOU STARTED
          </p>
          <h2
            className="text-3xl sm:text-4xl font-bold mb-6"
            data-testid="title"
          >
            Create an Account
          </h2>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6">
              <div>
                <Label
                  htmlFor="firstName"
                  className="mb-1 block text-gray-700 text-sm"
                >
                  First Name
                </Label>
                <Input
                  id="firstName"
                  placeholder="Enter First Name"
                  value={formData.firstName}
                  onChange={(e) => handleChange("firstName", e.target.value)}
                  className={`w-full h-12 p-3 border ${
                    errors.firstName
                      ? "border-red-500 focus:ring-red-300"
                      : "border-gray-300 focus:border-green-600 focus:ring focus:ring-green-300"
                  } rounded-lg`}
                />
                <p className="text-gray-500 text-xs mt-1">e.g.Janson</p>
                {errors.firstName && (
                  <p className="text-red-500 text-xs mt-1">
                    First name can be up to 50 characters and only contain Latin
                    letters, hyphens, and apostrophes are allowed.
                  </p>
                )}
              </div>
              <div>
                <Label
                  htmlFor="lastName"
                  className="mb-1 block text-gray-700 text-sm"
                >
                  Last Name
                </Label>
                <Input
                  id="lastName"
                  placeholder="Enter Last Name"
                  value={formData.lastName}
                  onChange={(e) => handleChange("lastName", e.target.value)}
                  className={`w-full h-12 p-3 border ${
                    errors.lastName
                      ? "border-red-500 focus:ring-red-300"
                      : "border-gray-300 focus:border-green-600 focus:ring focus:ring-green-300"
                  } rounded-lg`}
                />
                <p className="text-gray-500 text-xs mt-1">e.g.Doe</p>
                {errors.lastName && (
                  <p className="text-red-500 text-xs mt-1">
                    Last name can be up to 50 characters and only contain Latin
                    letters, hyphens, and apostrophes are allowed.
                  </p>
                )}
              </div>
            </div>

            <div className="mt-4 sm:mt-5">
              <Label
                htmlFor="email"
                className="mb-1 block text-gray-700 text-sm"
              >
                Email
              </Label>
              <Input
                id="email"
                type="email"
                placeholder="Enter Email"
                value={formData.email}
                onChange={(e) => handleChange("email", e.target.value)}
                className={`w-full h-12 p-3 border ${
                  errors.email
                    ? "border-red-500 focus:ring-red-300"
                    : "border-gray-300 focus:border-green-600 focus:ring focus:ring-green-300"
                } rounded-lg`}
              />
              <p className="text-gray-500 text-xs mt-1">
                e.g.username@domain.com
              </p>
              {errors.email && (
                <p className="text-red-500 text-xs mt-1">
                  Invalid email address. Please ensure it follows the format:
                  username@domain.com
                </p>
              )}
            </div>

            <div className="mt-4 sm:mt-5 relative">
              <Label
                htmlFor="password"
                className="mb-1 block text-gray-700 text-sm"
              >
                Password
              </Label>
              <Input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="Enter Password"
                value={formData.password}
                onChange={(e) => handleChange("password", e.target.value)}
                className={`w-full h-12 p-3 border ${
                  errors.password
                    ? "border-red-500 focus:ring-red-300"
                    : "border-gray-300 focus:border-green-600 focus:ring focus:ring-green-300"
                } rounded-lg`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute top-[38px] right-4 text-gray-500 hover:text-gray-700 focus:outline-none"
                data-testid="togglePassword"
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
              <ul className="text-xs mt-2 space-y-1">
                <li
                  className={`${
                    formData.password.length === 0
                      ? "text-black"
                      : passwordRules.hasUppercase
                      ? "text-green-600"
                      : "text-red-500"
                  }`}
                >
                  ● At least one uppercase letter required
                </li>
                <li
                  className={`${
                    formData.password.length === 0
                      ? "text-black"
                      : passwordRules.hasLowercase
                      ? "text-green-600"
                      : "text-red-500"
                  }`}
                >
                  ● At least one lowercase letter required
                </li>
                <li
                  className={`${
                    formData.password.length === 0
                      ? "text-black"
                      : passwordRules.hasNumber
                      ? "text-green-600"
                      : "text-red-500"
                  }`}
                >
                  ● At least one number required
                </li>
                <li
                  className={`${
                    formData.password.length === 0
                      ? "text-black"
                      : passwordRules.hasSpecialChar
                      ? "text-green-600"
                      : "text-red-500"
                  }`}
                >
                  ● At least one special character required
                </li>
                <li
                  className={`${
                    formData.password.length === 0
                      ? "text-black"
                      : passwordRules.isValidLength
                      ? "text-green-600"
                      : "text-red-500"
                  }`}
                >
                  ● Password must be 8-16 characters long
                </li>
              </ul>
            </div>

            <div className="mt-4 sm:mt-5 relative">
              <Label
                htmlFor="confirmPassword"
                className="mb-1 block text-gray-700 text-sm"
              >
                Confirm Password
              </Label>
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? "text" : "password"}
                placeholder="Confirm Password"
                value={formData.confirmPassword}
                onChange={(e) =>
                  handleChange("confirmPassword", e.target.value)
                }
                className={`w-full h-12 p-3 border ${
                  errors.confirmPassword
                    ? "border-red-500 focus:ring-red-300"
                    : "border-gray-300 focus:border-green-600 focus:ring focus:ring-green-300"
                } rounded-lg`}
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute top-[38px] right-4 text-gray-500 hover:text-gray-700 focus:outline-none"
                data-testid="toggleConfirmPassword"
              >
                {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
              {formData.confirmPassword.length === 0 ? (
                <p className="text-gray-500 text-xs mt-2">
                  • Must match the password
                </p>
              ) : formData.confirmPassword !== formData.password ? (
                <p className="text-red-500 text-xs mt-1">
                  Passwords do not match. Please try again.
                </p>
              ) : (
                <p className="text-green-500 text-xs mt-2">✓ Passwords match</p>
              )}
            </div>

            <Button
              type="submit"
              className="w-full mt-6 bg-green-600 text-white hover:bg-green-700 p-3 rounded-lg"
            >
              Create an Account
            </Button>
          </form>

          <p className="mt-4 text-xs sm:text-sm text-gray-600">
            Already have an account?{" "}
            <Link to="/login" className="text-blue-500 font-medium">
              Login
            </Link>
          </p>
        </div>

        {/* Right Section */}
        <div className="w-full hidden md:flex flex-col md:w-1/2  md:items-center md:justify-center bg-gray-100 p-6 sm:p-10">
          <div className="flex items-center justify-center space-x-2 mb-4 sm:mb-10">
            <h1 className="text-4xl sm:text-6xl md:text-4xl font-extrabold text-green-600">
              Green
            </h1>
            <h1 className="text-4xl sm:text-6xl md:text-4xl font-extrabold text-black">
              {" "}
              & Tasty
            </h1>
          </div>
          <img
            src={Logo}
            alt="Green & Tasty"
            className="w-32 sm:w-48 md:w-64 lg:w-80 h-auto mt-4 sm:mt-10"
          />
        </div>
      </div>
    </div>
  );
};

export default Registration;
