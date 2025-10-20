import { configureStore } from "@reduxjs/toolkit";
import locationsReducer from "./slice";
import tableSlice from './tableSlice'
import userReducer from './userProfile'
import locationsOptionsReducer from "./locationsOption";

export const store = configureStore({
    reducer: {
        locations: locationsReducer,
        tables: tableSlice,
        user: userReducer,
        locationsOption: locationsOptionsReducer
    },
    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware(),
})
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
export default store;