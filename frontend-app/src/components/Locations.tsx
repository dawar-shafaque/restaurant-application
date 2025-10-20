import { useEffect } from "react";
import { FaMapMarkerAlt } from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "@/redux/store";
import { setLocations, setLoading } from "@/redux/slice";
import { API } from "@/api/endpoints";

interface Locations {
    address:string;
    averageOccupancy:number;
    id:string;
    totalCapacity:number;
    imageUrl:string;

}

const Locations = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const {locations, loading} = useSelector((state: RootState) => state.locations)

  useEffect(() => {
    const FetchLocations = async () => {
      try {
        dispatch(setLoading(true));
        const response = await fetch(API.LOCATIONS, {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
          },
        });
        const responseData = await response.json();
        console.log(responseData)
        // setLocations(responseData["tm16-locations-dev1"]);
        // dispatch(setLocations(responseData["tm16-locations-dev6"]));
        dispatch(setLocations(responseData));
      } catch (error) {
        console.error(error);
      } finally {
        dispatch(setLoading(false));
      }
    };
    FetchLocations();
  }, [dispatch]);
  return (
    <section className="p-8">
      <h2 className="text-2xl font-semibold mb-4">Locations</h2>

      {/* Show Loading Spinner or Text */}
      {loading ? (
        <div className="text-center text-gray-600 text-lg">Loading...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {locations.map((loc, index) => (
            <div
              key={index}
              className="bg-white shadow-md hover:cursor-pointer transition delay-150 duration-300 ease-in-out hover:scale-90 rounded-lg p-4"
              onClick={() => navigate(`/location/${loc.id}`)}
            >
              <img
                src={loc.imageUrl}
                alt={loc.address}
                className="w-full h-40 object-cover rounded-md "
              />
              <div className="flex items-center">
                <FaMapMarkerAlt />
                <h3 className="m-2 font-medium">{loc.address}</h3>
              </div>
              <div className="flex justify-between items-center">
                <p className="text-gray-500">Total Capacity: {loc.totalCapacity}</p>
                <p className="text-gray-500">Average Occupancy: {loc.averageOccupancy}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
};

export default Locations;
