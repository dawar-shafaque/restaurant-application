import { API } from "@/api/endpoints";
import { useState } from "react";
import { toast } from "react-toastify";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { useNavigate } from "react-router-dom";

const ChangePassword = () => {
  const navigate = useNavigate();
  const [oldPassword, setOldPassword] = useState<string>("");
  const [newPassword, setNewPassword] = useState<string>("");
  const [confirmPassword, setConfirmPassword] = useState<string>("");

  const [showOld, setShowOld] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const [passwordCriteria, setPasswordCriteria] = useState({
    hasUppercase: false,
    hasLowercase: false,
    hasNumber: false,
    hasSpecialChar: false,
    isBetweenLength: false,
    notSameAsOld: null as boolean | null,
    passwordsMatch: false,
  });

  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newPassword = e.target.value;
    setNewPassword(newPassword);

    setPasswordCriteria((prev) => ({
      ...prev,
      hasUppercase: /[A-Z]/.test(newPassword),
      hasLowercase: /[a-z]/.test(newPassword),
      hasNumber: /\d/.test(newPassword),
      hasSpecialChar: /[^A-Za-z0-9]/.test(newPassword),
      isBetweenLength: newPassword.length >= 8 && newPassword.length <= 16,
      notSameAsOld: newPassword === oldPassword ? false : true,
      passwordsMatch: newPassword === confirmPassword,
    }));
  };

  const handleConfirmPasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const confirmPassword = e.target.value;
    setConfirmPassword(confirmPassword);

    setPasswordCriteria((prev) => ({
      ...prev,
      passwordsMatch: confirmPassword === newPassword,
    }));
  };

  const handleSaveChanges = async () => {
    if (
      !passwordCriteria.hasUppercase ||
      !passwordCriteria.hasLowercase ||
      !passwordCriteria.hasNumber ||
      !passwordCriteria.hasSpecialChar ||
      !passwordCriteria.isBetweenLength ||
      passwordCriteria.notSameAsOld === false ||
      !passwordCriteria.passwordsMatch
    ) {
      return;
    }
  
    const token = sessionStorage.getItem("token");
    if (!token) {
      toast.error("You are not authenticated. Please log in.");
      return;
    }
  
    const payload = { oldPassword, newPassword };
  
    try {
      const response = await fetch(`${API.PASSWORD}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });
  
      // Safe response parsing
      const rawText = await response.clone().text();
      let responseData;
  
      try {
        responseData = JSON.parse(rawText);
      } catch {
        responseData = { message: rawText || "Password changed successfully" };
      }
  
      if (!response.ok) {
        toast.error(responseData.message);
        return;
      }
  
      toast.success(responseData.message);
  
      // Reset state
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setPasswordCriteria({
        hasUppercase: false,
        hasLowercase: false,
        hasNumber: false,
        hasSpecialChar: false,
        isBetweenLength: false,
        notSameAsOld: null,
        passwordsMatch: false,
      });
  
      navigate('/');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : JSON.stringify(error);
      console.error("Change Password Error:", errorMessage);
      toast.error(`Error changing password: ${errorMessage}`);
    }
  };
  

  const isStrongPassword =
    passwordCriteria.hasUppercase &&
    passwordCriteria.hasLowercase &&
    passwordCriteria.hasNumber &&
    passwordCriteria.hasSpecialChar &&
    passwordCriteria.isBetweenLength &&
    passwordCriteria.notSameAsOld !== false;

  const renderPasswordInput = (
    label: string,
    value: string,
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => void,
    show: boolean,
    toggle: () => void
  ) => (
    <div className="relative">
      <label className="block text-sm font-medium" htmlFor={`${label}`}>{label}</label>
      <input
        type={show ? "text" : "password"}
        id={`${label}`}
        className="w-full border rounded px-3 py-2 mt-1 border-gray-300 focus:border-green-600 focus:ring focus:ring-green-300 hover:border-green-600 pr-10"
        value={value}
        onChange={onChange}
      />
      <span
        onClick={toggle}
        className="absolute top-[38px] right-3 cursor-pointer text-gray-600 hover:text-black"
      >
        {show ? <FaEyeSlash /> : <FaEye />}
      </span>
    </div>
  );

  return (
    <div className="w-full max-w-[800px]">
      <div className="space-y-4">
        {/* Old Password */}
        {renderPasswordInput("Old Password", oldPassword, (e) => setOldPassword(e.target.value), showOld, () => setShowOld(!showOld))}

        {/* New Password */}
        <div className="relative">
          {newPassword && (
            <p className={`absolute top-0 right-0 text-sm ${isStrongPassword ? "text-green-600" : "text-red-600"}`}>
              {isStrongPassword ? "● Strong " : "● Weak "}
            </p>
          )}
          {renderPasswordInput("New Password", newPassword, handlePasswordChange, showNew, () => setShowNew(!showNew))}
          <ul className="text-sm mt-2">
            <li className={passwordCriteria.hasUppercase ? "text-green-600" : "text-gray-500"}>
              ● At least one uppercase letter required
            </li>
            <li className={passwordCriteria.hasLowercase ? "text-green-600" : "text-gray-500"}>
              ● At least one lowercase letter required
            </li>
            <li className={passwordCriteria.hasNumber ? "text-green-600" : "text-gray-500"}>
              ● At least one number required
            </li>
            <li className={passwordCriteria.hasSpecialChar ? "text-green-600" : "text-gray-500"}>
              ● At least one special character required
            </li>
            <li className={passwordCriteria.isBetweenLength ? "text-green-600" : "text-gray-500"}>
              ● Password must be 8-16 characters long
            </li>
            <li className={
              passwordCriteria.notSameAsOld === null
                ? "text-gray-500"
                : passwordCriteria.notSameAsOld
                ? "text-green-600"
                : "text-red-600"
            }>
              ● New password should not match old password
            </li>
          </ul>
        </div>

        {/* Confirm Password */}
        <div>
          {renderPasswordInput(
            "Confirm New Password",
            confirmPassword,
            handleConfirmPasswordChange,
            showConfirm,
            () => setShowConfirm(!showConfirm)
          )}
          <ul className="text-sm mt-2">
            <li
              className={`${
                confirmPassword
                  ? passwordCriteria.passwordsMatch
                    ? "text-green-600"
                    : "text-gray-500"
                  : "text-gray-500"
              }`}
            >
              ● Confirm password must match the new password
            </li>
          </ul>
        </div>

        <div className="flex justify-end">
          <button
            onClick={handleSaveChanges}
            className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
            disabled={
              !passwordCriteria.hasUppercase ||
              !passwordCriteria.hasLowercase ||
              !passwordCriteria.hasNumber ||
              !passwordCriteria.hasSpecialChar ||
              !passwordCriteria.isBetweenLength ||
              passwordCriteria.notSameAsOld === false ||
              !passwordCriteria.passwordsMatch
            }
          >
            Save Changes
          </button>
        </div>
      </div>
    </div>
  );
};

export default ChangePassword;
