import ReactDOM from "react-dom";
import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "@/redux/store";
import { fetchLocationsOptions } from "@/redux/locationsOption";
import { API } from "@/api/endpoints";
import { format } from "date-fns";
import { toast } from "react-toastify";

interface ReservationModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const modalRoot = document.getElementById("modal-root") || document.body;

export const ReservationModal: React.FC<ReservationModalProps> = ({
  isOpen,
  onClose,
}) => {
  const [customerType, setCustomerType] = useState<"VISITOR" | "CUSTOMER">(
    "VISITOR"
  );
  const [guestCount, setGuestCount] = useState<number>(1);
  const [selectedLocation, setSelectedLocation] = useState<string>("");
  const [selectedTimeFrom, setSelectedTimeFrom] = useState<string>("12:15");
  const [selectedTimeTo, setSelectedTimeTo] = useState<string>("13:45");
  const [selectedTable, setSelectedTable] = useState<string>("T1");
  const [date, setDate] = useState(new Date());

  // const [customers, setCustomers] = useState([]);
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [filteredCustomers, setFilteredCustomers] = useState([]);

  const dispatch = useDispatch<AppDispatch>();
  const { locationsDev, loading, error } = useSelector(
    (state: RootState) => state.locationsOption
  );

  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleEscape);
    return () => document.removeEventListener("keydown", handleEscape);
  }, [onClose]);

  useEffect(() => {
    dispatch(fetchLocationsOptions());
  }, [dispatch]);
  

  useEffect(() => {
    const fetchFilteredCustomers = async () => {
      if (searchTerm.trim().length < 2) {
        setFilteredCustomers([]);
        return;
      }

      try {
        const response = await fetch(`${API.CUSTOMERS}?name=${searchTerm}`, {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sessionStorage.getItem('token')}`
          },
        });
        const responseData = await response.json();
        setFilteredCustomers(responseData || []);
      } catch (error) {
        console.error("Customer search error:", error);
        setFilteredCustomers([]);
      }
    };

    const debounceTimer = setTimeout(() => {
      fetchFilteredCustomers();
    }, 400); // debounce delay (ms)

    return () => clearTimeout(debounceTimer);
  }, [searchTerm]);

  const incrementGuests = () => {
    if (guestCount < 10) setGuestCount((prev) => prev + 1);
  };

  const decrementGuests = () => {
    if (guestCount > 0) setGuestCount((prev) => prev - 1);
  };

  const handleMakeReservation = async () => {
    console.log(
      selectedLocation,
      searchTerm,
      guestCount,
      selectedTimeFrom,
      selectedTimeTo,
      selectedTable,
      customerType,
      date
    );
    const reservationData = {
      clientType: customerType,
      customerName: searchTerm,
      locationId: selectedLocation,
      tableNumber: selectedTable,
      date: format(date, "yyyy-MM-dd"),
      timeFrom: selectedTimeFrom,
      timeTo: selectedTimeTo,
      guestsNumber: guestCount.toString(),


    }
    try {
      const response = await fetch(API.WAITER_BOOKING, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("token")}`,
        },
        body: JSON.stringify(reservationData),
      });
      const responseData = await response.json();
      if(!response.ok) {
        toast.error(responseData.message || "Failed to make reservation")
        throw new Error(responseData.message || "Failed to make reservation");
      } else {
        toast.success(responseData.message || "Reservation made successfully!");
      }
      
      onClose();
    } catch (error) {
      console.error(error);
    }
  };

  if (!isOpen) return null;

  return ReactDOM.createPortal(
    <div className="fixed inset-0 z-50 backdrop-blur-md bg-white/30 bg-opacity-50 flex items-center justify-center">
      <div className="bg-white rounded-2xl p-6 w-[400px] relative">
        <button
          className="absolute right-4 top-4 text-gray-500"
          onClick={onClose}
          data-testid="close"
        >
          <X />
        </button>
        <h2 className="text-xl font-semibold mb-4">New Reservation</h2>

        <div className="space-y-4">
          {/* Location Dropdown */}
          {loading ? (
            <p>Loading locations...</p>
          ) : error ? (
            <p className="text-red-500">Error loading locations</p>
          ) : (
            <select
              className="w-full border border-gray-300 rounded px-3 py-2"
              value={selectedLocation}
              onChange={(e) => setSelectedLocation(e.target.value)}
            >
              <option value="" disabled>
                Select Location
              </option>
              {locationsDev.map((location: { id: string; address: string }) => (
                <option key={location.id} value={location.id}>
                  {location.address}
                </option>
              ))}
            </select>
          )}

          {/* Radio Buttons */}
          <div className="flex items-center justify-between border border-gray-300 rounded px-3 py-2">
            <label>
              <input
                type="radio"
                name="customer"
                className="mr-2"
                checked={customerType === "VISITOR"}
                onChange={() => setCustomerType("VISITOR")}
              />
              Visitor
            </label>
            <label>
              <input
                type="radio"
                name="customer"
                className="mr-2"
                checked={customerType === "CUSTOMER"}
                onChange={() => setCustomerType("CUSTOMER")}
              />
              Existing Customer
            </label>
          </div>

          {/* Conditional Input with Debounced Search */}
          {customerType === "CUSTOMER" && (
            <div className="border border-green-500 rounded-lg p-4 space-y-2">
              <label className="block font-medium text-sm text-gray-700">
                Customer’s Name
              </label>
              <Input
                type="text"
                placeholder="Enter Customer’s Name"
                className="w-full"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
              <p className="text-xs text-gray-400">e.g. Janson Doe</p>

              {filteredCustomers.length > 0 && (
                <ul className="border border-gray-300 rounded mt-2 max-h-40 overflow-y-auto text-sm">
                  {filteredCustomers.map((customer, index) => (
                    <li
                      key={index}
                      className="px-3 py-2 hover:bg-gray-100 cursor-pointer"
                      onClick={() => {
                        setSearchTerm(customer);
                        setFilteredCustomers([]);
                      }}
                    >
                      {customer}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}

          {/* Guest Count */}
          <div className="flex items-center justify-between border border-gray-300 rounded px-3 py-2">
            <label>Guests</label>
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                variant="outline"
                onClick={decrementGuests}
                disabled={guestCount === 0}
              >
                −
              </Button>
              <span>{guestCount}</span>
              <Button
                size="sm"
                variant="outline"
                onClick={incrementGuests}
                disabled={guestCount === 10}
              >
                +
              </Button>
            </div>
          </div>

          <div>
            <label className="block mb-1 text-sm text-gray-600" htmlFor="date">Date</label>
            <Input
              type="date"
              id="date"
              value={format(date, "yyyy-MM-dd")}
              className="w-full"
              onChange={(e) => setDate(new Date(e.target.value))}
              min={new Date().toISOString().split("T")[0]}
            />
          </div>

          {/* Time From/To */}
          <div>
            <label className="block mb-1 text-sm text-gray-600">Time</label>
            <div className="flex gap-2">
              <input
                type="time"
                className="flex-1 border border-gray-300 rounded px-3 py-2"
                value={selectedTimeFrom}
                onChange={(e) => setSelectedTimeFrom(e.target.value)}
              />
              <input
                type="time"
                className="flex-1 border border-gray-300 rounded px-3 py-2"
                value={selectedTimeTo}
                onChange={(e) => setSelectedTimeTo(e.target.value)}
              />
            </div>
          </div>

          {/* Table Dropdown */}
          <select
            className="w-full border border-gray-300 rounded px-3 py-2"
            value={selectedTable}
            onChange={(e) => setSelectedTable(e.target.value)}
          >
            {[...Array(10)].map((_, idx) => (
              <option key={idx} value={`T${idx + 1}`}>
                T{idx + 1}
              </option>
            ))}
          </select>

          {/* Submit */}
          <Button
            className="w-full bg-green-600 text-white hover:bg-green-700"
            onClick={handleMakeReservation}
          >
            Make a Reservation
          </Button>
        </div>
      </div>
    </div>,
    modalRoot
  );
};
