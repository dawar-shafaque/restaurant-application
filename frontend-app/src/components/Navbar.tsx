import { useEffect, useState, useRef } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { Button } from "./ui/button";
import logo from "@/assets/vegetarian-_1_ 1.svg";
import { RxAvatar } from "react-icons/rx";
import { HiMenu, HiX } from "react-icons/hi";

export const Navbar = () => {
  const [isUser, setIsUser] = useState(false);
  const [username, setUsername] = useState("");
  const [role, setRole] = useState("");
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const token = sessionStorage.getItem("token");
    const storedUsername = sessionStorage.getItem("username");
    const storedRole = sessionStorage.getItem("role");

    if (token) {
      setIsUser(true);
      if (storedUsername) setUsername(storedUsername);
      if (storedRole) setRole(storedRole);
    }
  }, []);

  const handleLogout = () => {
    sessionStorage.removeItem("token");
    sessionStorage.removeItem("username");
    sessionStorage.removeItem("role");
    setIsUser(false);
    setUsername("");
    setRole("");
    setDropdownOpen(false);
    navigate("/login");
  };

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const renderLinks = () => {
    if (!isUser) {
      return (
        <>
          <NavLink to="/" className={navClass}>
            Main Page
          </NavLink>
          <NavLink to="/bookTable" className={navClass}>
            Book a Table
          </NavLink>
        </>
      );
    }

    if (role === "Waiter") {
      return (
        <>
          <NavLink to="/waiter-reservation" className={navClass}>
            Reservation
          </NavLink>
          <NavLink to="/view-menu" className={navClass}>
            Menu
          </NavLink>
        </>
      );
    }

    return (
      <>
        <NavLink to="/" className={navClass}>
          Main Page
        </NavLink>
        <NavLink to="/bookTable" className={navClass}>
          Book a Table
        </NavLink>
        <NavLink to="/reservation" className={navClass}>
          Reservation
        </NavLink>
      </>
    );
  };

  const navClass = ({ isActive }: { isActive: boolean }) =>
    isActive ? "text-green-500 font-bold underline" : "text-gray-600";

  return (
    <nav className="flex justify-between items-center px-6 py-4 shadow-md bg-white relative">
      {/* Logo Section */}
      <div className="flex items-center gap-4">
        <img src={logo} alt="Green & Tasty Logo" className="w-8 h-8" />
        <p className="text-s font-bold sm:text-xl text-green-600">
          Green & Tasty
        </p>
      </div>

      {/* Desktop Nav */}
      <div className="hidden md:flex gap-8">{renderLinks()}</div>

      {/* Mobile Menu Toggle */}
      <div className="md:hidden">
        <button onClick={() => setMobileMenuOpen((prev) => !prev)} data-testid="hamburger">
          {mobileMenuOpen ? (
            <HiX className="w-6 h-6 text-green-600" />
          ) : (
            <HiMenu className="w-6 h-6 text-green-600" />
          )}
        </button>
      </div>

      {/* User Avatar or Sign In Button */}
      {isUser ? (
        <div className="relative ml-4" ref={dropdownRef}>
          <RxAvatar
            data-testid="profileDropdown"
            onClick={() => setDropdownOpen(!dropdownOpen)}
            className="h-6 w-6 cursor-pointer"
            id="profileDropdown"
          />
          {dropdownOpen && (
            <div className="absolute right-0 mt-2 w-44 bg-white shadow-lg rounded-md border z-10">
              <p className="px-4 py-2 text-gray-600 font-semibold border-b">
                {username} ({role})
              </p>
              <Link
                to="/profile"
                className="block px-4 py-2 text-gray-700 hover:bg-gray-100"
                onClick={() => setDropdownOpen(false)}
              >
                My Profile
              </Link>
              <button
                className="w-full text-left px-4 py-2 text-gray-700 hover:bg-gray-100"
                onClick={handleLogout}
              >
                Sign Out
              </button>
            </div>
          )}
        </div>
      ) : (
        <Link to="/login" className="ml-4 hidden md:block">
          <Button
            variant="outline"
            className="hover:cursor-pointer text-green-600 border-green-500 font-semibold"
          >
            Sign In
          </Button>
        </Link>
      )}

      {/* Mobile Nav Menu */}
      {mobileMenuOpen && (
        <div className="absolute top-full left-0 w-full bg-white shadow-md z-10 flex flex-col px-6 py-4 gap-4 md:hidden">
          {renderLinks()}
          {!isUser && (
            <Link to="/login">
              <Button
                variant="outline"
                className="w-full text-green-600 border-green-500 font-semibold"
              >
                Sign In
              </Button>
            </Link>
          )}
        </div>
      )}
    </nav>
  );
};
