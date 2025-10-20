import { API } from "@/api/endpoints";
import { createSlice, createAsyncThunk, PayloadAction } from "@reduxjs/toolkit";

interface LocationType {
  address: string;
  id: string;
}

interface LocationsState {
  locationsDev: LocationType[];
  loading: boolean;
  error: string | null;
}

const initialState: LocationsState = {
  locationsDev: [],
  loading: false,
  error: null,
};

export const fetchLocationsOptions = createAsyncThunk(
  "locations/selectLocations",
  async () => {
    try {
      const response = await fetch(API.LOCATIONS_OPTIONS, {
        method: "GET",
        headers: { "Content-Type": "application/json" },
      });
      const responseData = await response.json();
      console.log(responseData["tm16-locations-dev6"]);
      return responseData;
    } catch (error) {
      console.error(error);
    }
  }
);

const locationsOptionsSlice = createSlice({
  name: "locationsOption",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchLocationsOptions.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(
        fetchLocationsOptions.fulfilled,
        (state, action: PayloadAction<LocationType[]>) => {
          state.locationsDev = action.payload;
          state.loading = false;
        }
      )
      .addCase(fetchLocationsOptions.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});
export default locationsOptionsSlice.reducer;