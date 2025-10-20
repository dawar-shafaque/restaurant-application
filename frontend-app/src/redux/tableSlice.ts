import { API } from '@/api/endpoints';
import { Table } from '@/types/FormData';
import {createAsyncThunk, createSlice, PayloadAction} from '@reduxjs/toolkit';
import { toast } from 'react-toastify';

interface TableState {
    tables: Table[];
    loading: boolean;
    error: string | null;
}

const initialTableState: TableState = {
    tables: [],
    loading: false,
    error: null,
}

export const fetchTables = createAsyncThunk("tables/fetchTables", async ({locationId, date, guests, time} : {locationId: string, date: string, guests: number, time:string}) => {
    if (!locationId) {
        const msg = "Location is required";
        toast.error(msg);
        throw new Error(msg);
      }
  
      if (!date) {
        const msg = "Date is required";
        toast.error(msg);
        throw new Error(msg);
      }
  
      if (!guests || guests <= 0) {
        const msg = "Guests must be at least 1";
        toast.error(msg);
        throw new Error(msg);
      }
  
      if (!time) {
        const msg = "Time is required";
        toast.error(msg);
        throw new Error(msg);
      }
    const apiUrl = `${API.TABLES}?locationId=${locationId}&date=${date}&guests=${guests}&time=${time}`;
    console.log(apiUrl)
    try {
        const response = await fetch(apiUrl, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
        });
        const data = await response.json();
        if (!response.ok) {
            toast.error(data.message);
            throw new Error(`Error: ${response.statusText}`);
        }
        return data;
    }catch(error) {
        console.error(error);
        throw error;
    }
});

const tableSlice = createSlice({
    name: "tables",
    initialState: initialTableState,
    reducers: {
        resetTables: (state) => {
            state.tables = [];
            state.loading = false;
            state.error = null;
        }
    },
    extraReducers: (builder) => {
        builder.addCase(fetchTables.pending, (state) => {
            state.loading = true;
            state.error = null;
        })
        .addCase(fetchTables.fulfilled, (state, action: PayloadAction<Table[]>) => {
            state.loading = false;
            state.tables = action.payload;
        })
        .addCase(fetchTables.rejected, (state, action) => {
            state.loading = false;
            state.error = action.error.message || "Failed to fetch tables";
        })
    }
})
export const {resetTables} = tableSlice.actions;
export default tableSlice.reducer;