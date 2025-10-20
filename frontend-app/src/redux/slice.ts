import { API } from "@/api/endpoints";
import { createAsyncThunk, createSlice, PayloadAction } from "@reduxjs/toolkit";

interface Location {
  address: string;
  description: string;
  averageOccupancy: number;
  id: string;
  totalCapacity: number;
  rating: string;
  imageUrl: string;
}

interface LocationsState {
  locations: Location[];
  loading: boolean;
}

const initialState: LocationsState = {
  locations: [],
  loading: false,
};
export const fetchLocations = createAsyncThunk('/fetch/locations', async () => {
  try {
    const response = await fetch(API.LOCATIONS, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });
    const data = await response.json();
    console.log(data);
    return data["tm16-locations-dev6"]
  }catch(error) {
    console.error(error);
  }
})
const locationsSlice = createSlice({
  name: "locations",
  initialState,
  reducers: {
    setLocations(state, action: PayloadAction<Location[]>) {
      state.locations = action.payload;
    },
    setLoading(state, action: PayloadAction<boolean>) {
      state.loading = action.payload;
    },
  },
});
export const { setLocations, setLoading } = locationsSlice.actions;
export default locationsSlice.reducer;
