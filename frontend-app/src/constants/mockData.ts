
export interface TableType {
    id:number,
    location:string,
    seating:number,
    slots: string[],
    image:string
}
export const dummyTables = [
    {
      id: 1,
      location: "48 Rustaveli Avenue",
      seating: 4,
      slots: ["10:30 a.m. - 12:00 p.m", "12:15 p.m. - 1:45 p.m", "2:00 p.m. - 3:30 p.m"],
      image: "https://source.unsplash.com/300x200/?restaurant",
    },
    {
      id: 2,
      location: "48 Rustaveli Avenue",
      seating: 4,
      slots: ["3:45 p.m. - 5:15 p.m", "5:30 p.m. - 7:00 p.m"],
      image: "https://source.unsplash.com/300x200/?restaurant",
    },
    {
      id: 3,
      location: "14 Baratashvili Street",
      seating: 6,
      slots: ["11:00 a.m. - 12:30 p.m", "1:00 p.m. - 2:30 p.m"],
      image: "https://source.unsplash.com/300x200/?restaurant",
    },
  ];
