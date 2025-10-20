import { API } from "@/api/endpoints";
import { createSlice, createAsyncThunk, PayloadAction } from "@reduxjs/toolkit";
import { toast } from "react-toastify";

interface UserProfile {
  firstName: string;
  lastName: string;
  userAvatarUrl: string;
}

export const fetchUserProfiles = createAsyncThunk(
  "user/fetchUserProfiles",
  async () => {
    try {
      const response = await fetch(API.USERS_PROFILE, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("token")}`,
        },
      });

      const data: UserProfile = await response.json();
      return data;
    } catch (error) {
      if (error instanceof Error) {
        throw new Error(error.message);
      } else {
        throw new Error("An unknown error occurred");
      }
    }
  }
);

export const updateUserProfile = createAsyncThunk(
  "user/updateUserProfile",
  async (userProfile: UserProfile) => {
    try {
      const response = await fetch(API.USERS_PROFILE, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("token")}`,
        },
        body: JSON.stringify(userProfile),
      });

      if (!response.ok) {
        throw new Error("Failed to update profile");
      }

      const contentType = response.headers.get("Content-Type");
      if(!response.ok) {
        toast.info(await response.text());
      } else toast.success(await response.text());
      let updatedProfile: UserProfile;

      if (contentType && contentType.includes("application/json")) {
        updatedProfile = await response.json();
      } else {
        // Fallback if backend only returns a success message
        updatedProfile = userProfile;
      }

      const username = `${updatedProfile.firstName} ${updatedProfile.lastName}`;
      sessionStorage.setItem("username", username);

      return updatedProfile;
    } catch (error) {
      if (error instanceof Error) {
        throw new Error(error.message);
      } else {
        throw new Error("An unknown error occurred");
      }
    }
  }
);

const userSlice = createSlice({
  name: "user",
  initialState: {
    profile: {} as UserProfile,
    loading: false,
    error: null as string | null,
  },
  reducers: {},
  extraReducers: (builder) => {
    builder
      // Fetch user profile
      .addCase(fetchUserProfiles.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchUserProfiles.fulfilled, (state, action: PayloadAction<UserProfile>) => {
        state.loading = false;
        state.profile = action.payload;
      })
      .addCase(fetchUserProfiles.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "Failed to fetch user profile";
      })

      // Update user profile
      .addCase(updateUserProfile.pending, (state) => {
        state.loading = true;
      })
      .addCase(updateUserProfile.fulfilled, (state, action: PayloadAction<UserProfile>) => {
        state.loading = false;
        state.profile = action.payload;
      })
      .addCase(updateUserProfile.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "Failed to update user profile";
      });
  },
});

export default userSlice.reducer;
