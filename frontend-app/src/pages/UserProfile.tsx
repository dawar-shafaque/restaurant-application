import { Navbar } from "@/components/Navbar";
import Group from "@/assets/Group.svg";
import { UserProfileData } from "@/types/FormData";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "@/redux/store";
import { fetchUserProfiles, updateUserProfile } from "@/redux/userProfile";
import ChangePassword from "@/components/Password";

const initialUserData: UserProfileData = {
  firstName: "",
  lastName: "",
  userAvatarUrl: "",
};

const UserProfile = () => {
  const [user, setUser] = useState<UserProfileData>(initialUserData);
  const [activeTab, setActiveTab] = useState<"general" | "password">("general");
  const dispatch = useDispatch<AppDispatch>();

  const { profile, loading, error } = useSelector(
    (state: RootState) => state.user
  );

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setUser((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  useEffect(() => {
    dispatch(fetchUserProfiles());
  }, [dispatch]);

  useEffect(() => {
    if (profile) {
      setUser({
        firstName: profile.firstName || "",
        lastName: profile.lastName || "",
        userAvatarUrl: profile.userAvatarUrl || "",
      });
    }
  }, [profile]);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onloadend = () => {
      const res = reader.result as string;
      const base64 = res.split(",")[1];
      setUser((prev) => ({
        ...prev,
        userAvatarUrl: base64,
      }));
    };
    reader.readAsDataURL(file);
  };

  const handleSubmit = () => {
    dispatch(updateUserProfile(user));
    setTimeout(() => {
      dispatch(fetchUserProfiles());
    }, 5000);
  };

  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />
      <div className="h-20 bg-gradient-to-r from-green-600 to-blue-500 flex items-center justify-between px-4 sm:px-8">
        <h1 className="text-white text-xl sm:text-2xl font-semibold">
          My Profile
        </h1>
        <img src={Group} alt="logo" className="h-10 sm:h-14" />
      </div>

      <div className="flex flex-col md:flex-row flex-1 bg-gray-50">
        {/* Sidebar */}
        <div className="w-full md:w-64 bg-white border-b md:border-b-0 md:border-r p-4 sm:p-6">
          <ul className="flex md:flex-col gap-4 justify-center md:justify-start">
            <li
              className={cn("cursor-pointer font-medium", {
                "text-green-600 underline underline-offset-4":
                  activeTab === "general",
              })}
              onClick={() => setActiveTab("general")}
            >
              General Information
            </li>
            <li
              className={cn("cursor-pointer font-medium", {
                "text-green-600 underline underline-offset-4":
                  activeTab === "password",
              })}
              onClick={() => setActiveTab("password")}
            >
              Change Password
            </li>
          </ul>
        </div>

        {/* Content */}
        <div className="flex-1 p-4 sm:p-6 lg:p-10">
          {loading ? (
            <p className="text-center text-gray-500">Loading profile...</p>
          ) : error ? (
            <p className="text-center text-red-500">Error: {error}</p>
          ) : (
            <>
              {activeTab === "general" && (
                <div className="bg-white border p-4 sm:p-6 rounded-lg shadow-md flex flex-col lg:flex-row gap-6">
                  {/* Profile Image */}
                  <div className="flex flex-col items-center gap-2">
                    <div className="w-24 h-24 sm:w-32 sm:h-32 rounded-full overflow-hidden border border-gray-300">
                      <img
                        src={user.userAvatarUrl || "/placeholder-avatar.png"}
                        alt="User"
                        className="object-cover w-full h-full"
                      />
                    </div>
                    <div className="text-sm flex flex-col items-center">
                      <label htmlFor="uploadPic" className="mb-1 text-gray-600">
                        Upload Photo
                      </label>
                      <input
                        type="file"
                        name="uploadPic"
                        id="uploadPic"
                        onChange={handleImageChange}
                        className="text-xs border border-green-500 px-2 py-1 rounded-md w-40"
                      />
                    </div>
                  </div>

                  {/* User Info Form */}
                  <div className="flex-1">
                    <div className="mb-4 flex gap-2">
                      <p className="font-semibold text-lg">
                        {user.firstName} {user.lastName}
                      </p>
                      <span className="font-semibold">
                        ({sessionStorage.getItem("role")})
                      </span>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label
                          className="block text-sm font-medium mb-1"
                          htmlFor="firstName"
                        >
                          First Name
                        </label>
                        <Input
                          id="firstName"
                          type="text"
                          name="firstName"
                          value={user.firstName}
                          onChange={handleChange}
                          placeholder="e.g. Jonson"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-1">
                          Last Name
                        </label>
                        <Input
                          type="text"
                          name="lastName"
                          value={user.lastName}
                          onChange={handleChange}
                          placeholder="e.g. Doe"
                        />
                      </div>
                    </div>

                    <div className="mt-6">
                      <Button
                        className="bg-green-600 hover:bg-green-700 text-white"
                        onClick={handleSubmit}
                      >
                        Save Changes
                      </Button>
                    </div>
                  </div>
                </div>
              )}

              {activeTab === "password" && (
                <div className="bg-white border p-6 rounded-lg shadow-md">
                  <ChangePassword />
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserProfile;
