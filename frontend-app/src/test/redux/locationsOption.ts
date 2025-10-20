import { fetchLocationsOptions } from "@/redux/locationsOption";
import { API } from "@/api/endpoints";
import { vi, describe, it, expect, beforeEach } from "vitest";
import { configureStore } from "@reduxjs/toolkit";
import locationsOptionsReducer from "@/redux/locationsOption";

vi.mock("@/api/endpoints", () => ({
  API: {
    LOCATIONS_OPTIONS: `${import.meta.env.VITE_BASE_URL}${import.meta.env.VITE_LOCATIONS_OPTIONS_API}`,
  },
}));

// Define the store configuration function
const createTestStore = () =>
  configureStore({
    reducer: {
      locationsOption: locationsOptionsReducer,
    },
  });

// Infer the RootState type from the store
type RootState = ReturnType<ReturnType<typeof createTestStore>["getState"]>;

describe("locationsOption slice", () => {
  let store: ReturnType<typeof createTestStore>;

  beforeEach(() => {
    store = createTestStore();
    vi.resetAllMocks();
    vi.clearAllMocks();
  });

  it("should handle fetchLocationsOptions.pending", () => {
    const action = fetchLocationsOptions.pending("", undefined);
    store.dispatch(action);

    const state: RootState = store.getState();
    expect(state.locationsOption.loading).toBe(true);
    expect(state.locationsOption.error).toBeNull();
  });

  it("should handle fetchLocationsOptions.fulfilled", async () => {
    const mockLocations = [
      { address: "123 Main St", id: "1" },
      { address: "456 Elm St", id: "2" },
    ];

    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve(mockLocations),
      } as Response)
    );

    await store.dispatch(fetchLocationsOptions() as unknown as ReturnType<typeof fetchLocationsOptions>);

    const state: RootState = store.getState();
    expect(state.locationsOption.loading).toBe(false);
    expect(state.locationsOption.locationsDev).toEqual(mockLocations);
    expect(state.locationsOption.error).toBeNull();
    expect(global.fetch).toHaveBeenCalledWith(API.LOCATIONS_OPTIONS, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
  });

  it("should handle fetchLocationsOptions.rejected", async () => {
    const mockError = "Failed to fetch locations";

    global.fetch = vi.fn(() => Promise.reject(new Error(mockError)));

    await store.dispatch(fetchLocationsOptions() as unknown as ReturnType<typeof fetchLocationsOptions>);

    const state: RootState = store.getState();
    expect(state.locationsOption.loading).toBe(false);
    expect(state.locationsOption.error).toBe(mockError);
    expect(state.locationsOption.locationsDev).toEqual([]);
  });
});
