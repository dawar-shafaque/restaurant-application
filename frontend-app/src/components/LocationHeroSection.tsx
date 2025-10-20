import { FaMapMarkerAlt, FaStar } from "react-icons/fa";
import { Link, useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import { RootState } from "@/redux/store";

const LocationHeroSection = () => {
  const {id} = useParams();
  const location = useSelector((state: RootState) => state.locations.locations.find((loc) => loc.id === id?.trim()));
  
  return (
    <div className="mx-auto my-8 p-4  flex flex-col md:flex-row items-center md:items-start">
      {/* Left Side: Text Information */}
      <div className="w-full md:w-1/2 p-4">
        <h1 className="text-green-600 text-3xl font-bold">Green & tasty</h1>
        <div className="flex items-center mt-2">
          <FaMapMarkerAlt className="text-gray-600" />
          <p className="ml-2 text-gray-700 font-medium">{location?.address}</p>
          <span className="ml-auto flex items-center text-yellow-500 font-bold">
            {location?.rating} <FaStar className="ml-1" />
          </span>
        </div>
        <p className="text-gray-600 mt-4 leading-relaxed">
          {location?.description}
        </p>
        <button className="mt-6 bg-green-600 text-white px-6 py-2 rounded-md text-lg font-medium hover:bg-green-700">
          <Link to="/bookTable">Book a Table</Link>
        </button>
      </div>

      {/* Right Side: Image */}
      <div className="w-full md:w-1/2 p-4">
        <img
          src={location?.imageUrl}
          alt="Image"
          className="w-full object-cover h-70 rounded-md shadow-lg"
        />
      </div>
    </div>
  );
};

export default LocationHeroSection;
