import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Calendar } from "lucide-react";
import { ReservationModal } from "./ReservationModal";
import {  useState } from "react";
import { format } from "date-fns";


interface ReservationFilterProps {
  onSearch: (date: string, time: string, tableNumber: string) => void;
}

export const ReservationFilter: React.FC<ReservationFilterProps> = ({
  onSearch,
}) => {
 
  const [showModal, setShowModal] = useState(false);
  const [date, setDate] = useState(new Date());
  const [timeSlot, setTimeSlot] = useState("00:00");
  const [tableNumber, setTableNumber] = useState("Any Table");
  
  
  return (
    <>
      <div className="flex flex-wrap items-center gap-4">
        <div className="flex items-center gap-2">
          <Calendar className="w-5 h-5 text-gray-500" />
          <Input type="date" value={format(date, "yyyy-MM-dd")} className="w-[160px]" onChange={(e) => setDate(new Date(e.target.value))} min={new Date().toISOString().split("T")[0]} data-testid="datePicker"/>
        </div>

        <select className="border border-gray-300 rounded px-3 py-2" value={timeSlot} onChange={(e) => setTimeSlot(e.target.value)}>
          <option>{timeSlot}</option>
            {["10:30", "12:15", "14:00", "15:45", "17:30", "19:15", "21:00"].map((time) => (
            <option key={time} value={time} data-testid="timeSlot">
              {time}
            </option>
            ))}
        </select>

        <select className="border border-gray-300 rounded px-3 py-2" value={tableNumber} onChange={(e) => setTableNumber(e.target.value)}>
          <option >{tableNumber}</option>
          {['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10'].map((table) => (
            <option key={table} value={table}>
              {table}
            </option>
          ))}
        </select>

        <Button variant="outline" onClick={() => onSearch(format(date, "yyyy-MM-dd"), timeSlot, tableNumber)}>
          Search
        </Button>

        <Button
          className="ml-auto bg-green-600 hover:bg-green-700 text-white"
          onClick={() => setShowModal(true)}
        >
          + Create New Reservation
        </Button>
      </div>
      <ReservationModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
      />
    </>
  );
};
