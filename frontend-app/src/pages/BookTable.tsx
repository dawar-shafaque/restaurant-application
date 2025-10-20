import { API } from "@/api/endpoints";
import { Navbar } from "@/components/Navbar";
import TableList from "@/components/TableList";
import { AppDispatch, RootState } from "@/redux/store";
import { fetchTables, resetTables } from "@/redux/tableSlice";
import { useEffect, useState } from "react";
import {
  FaMapMarkerAlt,
  FaClock,
  FaUser,
  FaMinus,
  FaPlus,
} from "react-icons/fa";
import { RiArrowDropDownLine } from "react-icons/ri";
import { useDispatch, useSelector } from "react-redux";

interface LocationType {
  address: string;
  id: string;
}

const BookTable = () => {
  const dispatch = useDispatch<AppDispatch>();
  const { tables } = useSelector((state: RootState) => state.tables);
  const [locations, setLocations] = useState<LocationType[]>([]);
  const [selectedLocation, setSelectedLocation] = useState<string>("");
  const [date, setDate] = useState<string>("");
  const [time, setTime] = useState<string>("00:00");
  const [guests, setGuests] = useState<number>(1);
  const [showLocationDropdown, setShowLocationDropdown] =
    useState<boolean>(false);
  const [locationId, setLocationId] = useState("");
  const [, setAddress] = useState("");

  const handleFindTable = async () => {
    dispatch(fetchTables({ locationId, date, guests, time }));
    console.log(time);
    // const apiUrl = `${
    //   API.TABLES
    // }?locationId=${locationId}&date=${date}&guests=${guests}&time=${time}`;
    // console.log(apiUrl)
    // try {
    //   const response = await fetch(apiUrl, {
    //     method: "GET",
    //     headers: { "Content-Type": "application/json" },
    //   });

    //   if (!response.ok) {
    //     throw new Error(`Error: ${response.statusText}`);
    //   }

    //   const responseData = await response.json();
    //   setTables(responseData);
    // } catch (error) {
    //   console.error(error);
    // }
  };

  useEffect(() => {
    const FetchLocationsOptions = async () => {
      try {
        const response = await fetch(API.LOCATIONS_OPTIONS, {
          method: "GET",
          headers: { "Content-Type": "application/json" },
        });
        const responseData = await response.json();
        setLocations(responseData);
        setAddress(responseData["tm16-locations-dev1"][0].address);
      } catch (error) {
        console.error(error);
      }
    };
    FetchLocationsOptions();
  }, []);
  useEffect(() => {
    return () => {
      dispatch(resetTables());
    };
  }, [dispatch]);

  return (
    <div>
      <Navbar />
      <div className="relative bg-black/80 p-4 md:p-8 rounded-lg w-full mx-auto text-white mt-2">
        <h2 className="text-green-500 text-lg font-bold text-center">
          Green & Tasty Restaurants
        </h2>
        <h1 className="text-3xl font-bold mb-6 text-center">Book a Table</h1>

        {/* Responsive Input Fields */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 justify-center">
          {/* Location Selector */}
          <div className="relative w-full max-w-[300px] mx-auto">
            <button
              className="flex items-center justify-between bg-white text-black px-4 py-3 rounded-lg w-full"
              onClick={() => setShowLocationDropdown(!showLocationDropdown)}
            >
              <div className="flex items-center gap-2">
                <FaMapMarkerAlt />
                <span className="overflow-hidden whitespace-nowrap text-ellipsis block max-w-[180px]">
                  {selectedLocation || "Location"}
                </span>
              </div>
              <RiArrowDropDownLine />
            </button>
            {showLocationDropdown && (
              <ul className="absolute left-0 w-full bg-white text-black mt-1 rounded-lg shadow-lg z-10">
                {locations.map((loc) => (
                  <li
                    key={loc.id}
                    className="p-2 hover:bg-green-100 cursor-pointer"
                    onClick={() => {
                      console.log(loc.id);
                      setSelectedLocation(loc.address);
                      setLocationId(loc.id);
                      setShowLocationDropdown(false);
                    }}
                  >
                    {loc.address}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Date Picker */}
          <div className="relative w-full max-w-[300px] mx-auto">
            <label className="flex items-center gap-2 bg-white text-black px-4 py-3 rounded-lg w-full">
              <input
                type="date"
                className="bg-transparent outline-none w-full"
                value={date}
                min={new Date().toISOString().split("T")[0]}
                onChange={(e) => setDate(e.target.value)}
              />
            </label>
          </div>

          {/* Time Picker */}
          <div className="relative w-full max-w-[300px] mx-auto">
            <label className="flex items-center gap-2 bg-white text-black px-4 py-3 rounded-lg w-full">
              <FaClock />
              <input
                type="time"
                className="bg-transparent outline-none w-full"
                value={time}
                onChange={(e) => setTime(e.target.value)}
              />
            </label>
          </div>

          {/* Guest Counter */}
          <div data-testid="guests" className="flex items-center justify-between bg-white text-black px-4 py-3 rounded-lg w-full max-w-[300px] mx-auto">
            <FaUser />
            <button
              className="p-1 rounded-full bg-gray-200 hover:bg-gray-300"
              onClick={() => setGuests((prev) => Math.max(1, prev - 1))}
              data-testid="decrement"
            >
              <FaMinus />
            </button>
            <span data-testid="guestCount">{guests}</span>
            <button
              className="p-1 rounded-full bg-gray-200 hover:bg-gray-300"
              onClick={() => setGuests((prev) => prev + 1)}
              data-testid="increment"
            >
              <FaPlus />
            </button>
          </div>

          {/* Find a Table Button */}
          <div className="w-full max-w-[300px] mx-auto">
            <button
              className="bg-green-500 text-white px-6 py-3 w-full rounded-lg hover:bg-green-600"
              onClick={handleFindTable}
            >
              Find a Table
            </button>
          </div>
        </div>
      </div>

      {/* Table List */}
      <div className="p-4">
        <TableList tables={tables} date={date} />
      </div>
    </div>
  );
};

export default BookTable;
