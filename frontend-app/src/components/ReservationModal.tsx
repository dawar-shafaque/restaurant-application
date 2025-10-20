import { API } from "@/api/endpoints";
import { useEffect } from "react";
import { createPortal } from "react-dom";
import { FaClock, FaMinus, FaPlus, FaTimes, FaUsers } from "react-icons/fa";
import { toast } from "react-toastify";

interface ReservationModalProps {
  isOpen: boolean;
  onClose: () => void;
  location: string;
  date: string;
  guests: number;
  setGuests: React.Dispatch<React.SetStateAction<number>>;
  timeSlot: string;
  tableNumber: string;
  locationId: string;
  tableCap: string;
}
const ReservationModal = ({
  isOpen,
  onClose,
  location,
  date,
  guests,
  setGuests,
  timeSlot,
  tableNumber,
  locationId,
  tableCap,
}: ReservationModalProps) => {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "auto";
    }
  }, [isOpen]);
  const handleReservation = async () => {
    const [timeFrom, timeTo] = timeSlot.split(" - ").map((t) => t.trim());
    const userData = {
      locationId,
      tableNumber,
      date,
      guestsNumber: guests,
      timeFrom,
      timeTo,
    };
    try {
      if (guests > parseInt(tableCap)) {
        toast.error("Number of guests exceeds table capacity.");
        onClose();
        throw new Error("Number of guests exceeds table capacity.");
      }
      const response = await fetch(API.BOOKING_CLIENTS, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${sessionStorage.getItem("token")}`,
        },
        body: JSON.stringify(userData),
      });
      if (!response.ok) {
        const reponseMsg = await response.json();
        toast.error(reponseMsg.message);
        onClose();
        throw new Error("Failed to make a reservation");
      }
      toast.success(
        (await response.json().then((res) => res.message)) ||
          "Reservation made successfully!"
      );
      onClose();
    } catch (error) {
      console.error(error);
    }
  };
  if (!isOpen) return null;
  return createPortal(
    <>
      <div className="fixed inset-0 flex items-center justify-center backdrop-blur-md bg-white/30 z-50">
        <div className="bg-white p-6 rounded-lg shadow-lg w-[400px] relative">
          {/* Close Button */}
          <button
            className="absolute top-3 right-3 text-gray-600 hover:text-gray-800"
            onClick={onClose}
            data-testid="closeButton"
          >
            <FaTimes size={20} />
          </button>

          {/* Title */}
          <h2 className="text-2xl font-semibold mb-2">Make a Reservation</h2>
          <p className="text-gray-600 mb-4">
            You are making a reservation at <strong>{location}</strong> for{" "}
            <strong>{date}</strong>.
          </p>

          {/* Guests Section */}
          <div className="mb-4">
            <p className="font-medium text-gray-700">Guests</p>
            <p className="text-gray-500 text-sm">
              Please specify the number of guests.
            </p>
            <div className="flex items-center border border-gray-300 p-2 rounded-lg mt-2">
              <FaUsers className="text-green-600 mr-2" />
              <button
                className="px-3 text-green-600"
                onClick={() => setGuests(Math.max(1, guests - 1))}
                data-testid="decrement"
              >
                <FaMinus />
              </button>
              <span className="mx-2" data-testid="guestCount">
                {guests}
              </span>
              <button
                className="px-3 text-green-600"
                onClick={() => {
                  if (guests < 10) setGuests(guests + 1);
                }}
                disabled={guests > 10}
                data-testid="increment"
              >
                <FaPlus />
              </button>
            </div>
          </div>

          {/* Time Slot Selection */}
          <div className="mb-4">
            <p className="font-medium text-gray-700">Time</p>
            <div className="flex items-center border border-gray-300 p-2 rounded-lg mt-2">
              <FaClock className="text-green-600 mr-2" />
              <span>{timeSlot}</span>
            </div>
          </div>

          {/* Reservation Button */}
          <button
            className={`w-full text-white p-2 rounded-lg ${
              guests > parseInt(tableCap)
                ? "bg-gray-400 cursor-not-allowed"
                : "bg-green-600 hover:bg-green-700"
            }`}
            onClick={handleReservation}
            disabled={guests > parseInt(tableCap)}
          >
            Make a Reservation
          </button>
          {guests > parseInt(tableCap) && (
            <p className="text-red-600 text-sm mt-2">
              Number of guests exceeds the table capacity ({tableCap}).
            </p>
          )}
        </div>
      </div>
    </>,
    document.body
  );
};
export default ReservationModal;
